"""Samsung Tizen (2016+) network remote: sends remote-control keys over the TV's websocket API.

    python3 -I samsung_remote.py <tv ip> KEY_MENU KEY_DOWN KEY_ENTER ...   (a number = pause in seconds)

The first connection makes the TV ask "Allow"; the token it returns is kept in samsung_token.txt next to this
script (a credential for this TV: never commit it). Standard library only: TLS websocket on port 8002.
"""
import base64
import json
import os
import socket
import ssl
import struct
import sys
import time

APP_NAME = base64.b64encode(b"tv-harper").decode()
TOKEN_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "samsung_token.txt")


def ws_connect(ip, token):
    path = f"/api/v2/channels/samsung.remote.control?name={APP_NAME}"
    if token:
        path += f"&token={token}"
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE   # the TV's certificate is self-signed
    sock = ctx.wrap_socket(socket.create_connection((ip, 8002), timeout=40), server_hostname=ip)
    key = base64.b64encode(os.urandom(16)).decode()
    sock.sendall((f"GET {path} HTTP/1.1\r\nHost: {ip}:8002\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
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


def ws_send(sock, text):
    data = text.encode()
    mask = os.urandom(4)
    header = bytes([0x81])
    n = len(data)
    if n < 126:
        header += bytes([0x80 | n])
    elif n < 65536:
        header += bytes([0x80 | 126]) + struct.pack(">H", n)
    else:
        header += bytes([0x80 | 127]) + struct.pack(">Q", n)
    sock.sendall(header + mask + bytes(b ^ mask[i % 4] for i, b in enumerate(data)))


def ws_recv(sock):
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
    mask = read(4) if b2 & 0x80 else None
    data = read(n)
    if mask:
        data = bytes(b ^ mask[i % 4] for i, b in enumerate(data))
    return data.decode(errors="replace")


def main():
    ip, actions = sys.argv[1], sys.argv[2:]
    token = open(TOKEN_FILE).read().strip() if os.path.exists(TOKEN_FILE) else ""
    sock = ws_connect(ip, token)
    hello = json.loads(ws_recv(sock))   # waits while the TV shows the Allow prompt
    if hello.get("event") != "ms.channel.connect":
        sys.exit(f"not allowed: {hello}")
    new = hello.get("data", {}).get("token")
    if new and new != token:
        with open(TOKEN_FILE, "w") as f:
            f.write(new)
        os.chmod(TOKEN_FILE, 0o600)
        print("token saved")
    for action in actions:
        try:
            time.sleep(float(action))
            continue
        except ValueError:
            pass
        ws_send(sock, json.dumps({"method": "ms.remote.control", "params": {
            "Cmd": "Click", "DataOfCmd": action, "Option": "false", "TypeOfRemote": "SendRemoteKey"}}))
        print("sent", action)
        time.sleep(0.6)
    sock.close()


if __name__ == "__main__":
    main()
