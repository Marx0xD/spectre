import paramiko

from app.Models.ProbleModel import ProbeModel
from app.schema.DeploymentConfig import SSHReachabilityResult
from app.utils.parse_os_release import parse_os_release
import socket

class SystemInspector:
   def __init__(self,host:ProbeModel | None):
       self._host = host
   def host_bootstrap_inspection(self)-> dict[str, str]:
       client = paramiko.SSHClient()
       client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
       hostname,ipaddress,sshUsername,sshPassword = self._host.model_dump().values()

       try:
           client.connect(
               hostname=ipaddress,
               username=sshUsername,
               password=sshPassword,
               timeout=10
           )

           stdin, stdout, stderr = client.exec_command("cat /etc/os-release")

           exit_status = stdout.channel.recv_exit_status()
           output = stdout.read().decode().strip()
           error = stderr.read().decode().strip()

           if exit_status != 0 or not output:
               raise RuntimeError(
                   f"Failed to read /etc/os-release: {error or 'empty output'}"
               )

           result = parse_os_release(output)

           return result

       finally:
           client.close()
   def check_container_health(self):
        pass
   @staticmethod
   def verify_ssh_reachable(host:str,port:int = 22)->SSHReachabilityResult:

       try:
           with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
               s.connect((host,port))
               ssh_banner = s.recv(1024).decode()
               ssh_result:SSHReachabilityResult  = SSHReachabilityResult(reachable=True,reason="ok",ssh_port=port,banner=ssh_banner)
               return ssh_result
       except ConnectionRefusedError as e:
           ssh_result: SSHReachabilityResult = SSHReachabilityResult(reachable=False, reason="connection_refused", ssh_port=port,banner=None)
           return ssh_result