import paramiko

from app.Models.ProbleModel import ProbeModel
from app.utils.parse_os_release import parse_os_release

class SystemInspector:
   def __init__(self,host:ProbeModel):
       self._host = host
   def probe(self)-> dict[str, str]:
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