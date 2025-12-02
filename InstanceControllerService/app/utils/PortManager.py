import paramiko
import socket



class PortManager:
    @staticmethod
    def get_open_tcp_ports(self):
        tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        tcp.bind(('', 0))
        return tcp.getsockname()

    async def get_remote_server_open_ports(self,host,username,password):
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy)
        client.connect(hostname=host, username=username, password=password)
        stdin, stdout, stderr = client.exec_command(
            "python3 -c 'import socket; s=socket.socket(); s.bind((\"\",0)); print(s.getsockname()[1]); s.close()'"
        )
        port = int(stdout.read().decode().strip())
        client.close()
        return port

