"""LG webOS network client (SSAP over a websocket): pairs with the TV, then sends one request.

    python3 -I lg_remote.py <ip> info                         system and software information
    python3 -I lg_remote.py <ip> apps                         installed apps (id, title)
    python3 -I lg_remote.py <ip> raw <ssap uri> [json payload]  any request, e.g. ssap://system.launcher/open
    python3 -I lg_remote.py <ip> key <HOME|MENU|UP|DOWN|LEFT|RIGHT|ENTER|BACK>   a remote button

The first connection makes the TV ask to allow it; the key it returns is kept in lg_key.txt next to this script
(a credential for this TV: never commit it). Standard library only: plain websocket on port 3000.
"""
import base64
import json
import os
import socket
import struct
import sys
import time

KEY_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "lg_key.txt")

# No signed manifest: the TV pairs on a prompt and grants only the permissions that need no LG signature.
MANIFEST = {
    "manifestVersion": 1,
    "appVersion": "1.0",
    "permissions": ["LAUNCH", "LAUNCH_WEBAPP", "CLOSE", "CONTROL_AUDIO", "CONTROL_INPUT_JOYSTICK",
                    "CONTROL_INPUT_TEXT", "CONTROL_MOUSE_AND_KEYBOARD", "CONTROL_POWER", "READ_APP_STATUS",
                    "READ_CURRENT_CHANNEL", "READ_INPUT_DEVICE_LIST", "READ_NETWORK_STATE", "READ_POWER_STATE",
                    "READ_RUNNING_APPS", "WRITE_NOTIFICATION_TOAST"],
}


def connect(ip):
    sock = socket.create_connection((ip, 3000), timeout=60)
    key = base64.b64encode(os.urandom(16)).decode()
    sock.sendall((f"GET / HTTP/1.1\r\nHost: {ip}:3000\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                  f"Sec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n\r\n").encode())
    head = b""
    while b"\r\n\r\n" not in head:
        chunk = sock.recv(1)
        if not chunk:
            raise ConnectionError("closed during handshake")
        head += chunk
    if b" 101 " not in head.split(b"\r\n", 1)[0]:
        raise ConnectionError(head.decode(errors="replace"))
    return sock


def send(sock, text):
    data = text.encode()
    mask = os.urandom(4)
    n = len(data)
    header = bytes([0x81]) + (bytes([0x80 | n]) if n < 126 else bytes([0x80 | 126]) + struct.pack(">H", n))
    sock.sendall(header + mask + bytes(b ^ mask[i % 4] for i, b in enumerate(data)))


def recv(sock):
    def read(n):
        buf = b""
        while len(buf) < n:
            chunk = sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("closed")
            buf += chunk
        return buf
    b1, b2 = read(2)
    n = b2 & 0x7F
    if n == 126:
        n = struct.unpack(">H", read(2))[0]
    elif n == 127:
        n = struct.unpack(">Q", read(8))[0]
    return json.loads(read(n).decode(errors="replace"))


def request(sock, uri, payload=None, rid="r"):
    msg = {"type": "request", "id": rid, "uri": uri}
    if payload is not None:
        msg["payload"] = payload
    send(sock, json.dumps(msg))
    while True:
        reply = recv(sock)
        if reply.get("id") == rid:
            return reply


def register(sock):
    key = open(KEY_FILE).read().strip() if os.path.exists(KEY_FILE) else None
    payload = {"forcePairing": False, "pairingType": "PROMPT", "manifest": MANIFEST}
    if key:
        payload["client-key"] = key
    send(sock, json.dumps({"type": "register", "id": "reg", "payload": payload}))
    while True:
        reply = recv(sock)
        if reply.get("type") == "registered":
            new = reply["payload"].get("client-key")
            if new and new != key:
                with open(KEY_FILE, "w") as f:
                    f.write(new)
                os.chmod(KEY_FILE, 0o600)
                print("key saved")
            return
        if reply.get("type") == "error":
            sys.exit(f"not allowed: {reply}")


def main():
    ip, cmd, rest = sys.argv[1], sys.argv[2], sys.argv[3:]
    sock = connect(ip)
    register(sock)
    if cmd == "info":
        for uri in ("ssap://system/getSystemInfo", "ssap://com.webos.service.update/getCurrentSWInformation",
                    "ssap://api/getServiceList"):
            print(uri, json.dumps(request(sock, uri).get("payload"), ensure_ascii=False))
    elif cmd == "apps":
        apps = request(sock, "ssap://com.webos.applicationManager/listApps").get("payload", {}).get("apps", [])
        for a in sorted(apps, key=lambda a: a.get("title", "")):
            print(f'{a.get("id")}\t{a.get("title")}\tsystem={a.get("systemApp")}\tvisible={a.get("visible")}')
    elif cmd == "key":
        socket_path = request(sock, "ssap://com.webos.service.networkinput/getPointerInputSocket") \
            .get("payload", {}).get("socketPath")
        if not socket_path:
            sys.exit("the TV gave no pointer socket (permission?)")
        host = socket_path.split("//", 1)[1]
        hostport, path = host.split("/", 1)
        psock = socket.create_connection((hostport.split(":")[0], int(hostport.split(":")[1])), timeout=10)
        wskey = base64.b64encode(os.urandom(16)).decode()
        psock.sendall((f"GET /{path} HTTP/1.1\r\nHost: {hostport}\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                       f"Sec-WebSocket-Key: {wskey}\r\nSec-WebSocket-Version: 13\r\n\r\n").encode())
        reply = b""
        while b"\r\n\r\n" not in reply:
            reply += psock.recv(1)
        for name in rest:
            send(psock, f"type:button\nname:{name.upper()}\n\n")
            print("sent", name.upper())
            time.sleep(0.6)
        psock.close()
    elif cmd == "raw":
        print(json.dumps(request(sock, rest[0], json.loads(rest[1]) if len(rest) > 1 else None), ensure_ascii=False))
    sock.close()


if __name__ == "__main__":
    main()
