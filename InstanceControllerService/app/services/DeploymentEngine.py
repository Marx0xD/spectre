import logging
import os
from logging import exception

from app.Scripts.InstallDocker import IntializationScript, startup_container
import paramiko

from app.schema.DeploymentConfig import DPInitializationResponseScheme, EnviromentSetupStatus, \
    DPContainerCreationResponseScheme
from app.schema.ResponseSchema import failed_response, success_response
from app.services.ConfigurationFileManager import ConfigurationFileManager
from app.services.DeploymentEngineBase import DeploymentEngineBase
from app.services.SymetricEncryptionService.decrypt_ssh_key import decrypt_ssh_password
from app.utils.PortManager import PortManager
from app.utils.load_server_intialize_script import load_server_exec_script

logger = logging.getLogger("dep engine")

class RemoteDeploymentEngine(DeploymentEngineBase):
    def __init__(self,username,password,host,idempotency_key):
        self._host=host
        self._username=username
        self._password = password
        self.port_manager = PortManager()
        self.idempotencyKey = idempotency_key


    def __connect_to_client(self):
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy)
        try:

            client.connect(
                hostname=self._host,
                username=self._username,
                password=self._password,
                look_for_keys=False,
                allow_agent=False,
            )
        except paramiko.AuthenticationException:
            return  None
        return client
    def get_ssh_client(self):
        return self.__connect_to_client()
    def initialize_server(self):
        print("running service")
        client = self.__connect_to_client()
        server_init_response = DPInitializationResponseScheme()
        master_secret = os.environ["SPECTRUM_SSH_MASTER_KEY"].encode("utf-8")
        password =  decrypt_ssh_password(encrypted_ssh_key=self._password,master_secret=master_secret)

        if not client:
            return failed_response(message="Authentication Error Check Your Credentials")

        deployment_script = load_server_exec_script("fedora")
        command = f"USERNAME={self._username} sudo -S -E bash -s"
        stdin, stdout, stderr = client.exec_command(command)

        # 1. Send sudo password
        stdin.write(password + "\n")
        stdin.flush()

        # 2. Send the script
        stdin.write(deployment_script)
        stdin.flush()

        stdin.channel.shutdown_write()
        ## properly analyze these responses for an optimal return
        print(stdout.read().decode())
        print(stderr.read().decode())
        output = stdout.read().decode() + stderr.read().decode()
        #once this is done create a folder in opt and give access 777 to cu


        if "SUCCESS: Docker CLI is installed" in output:
            server_init_response.config_dir = "/opt/ContainerConfiguration/"
            server_init_response.status = EnviromentSetupStatus.success
            server_init_response.message = output



        else:
            print("Docker installation failed!")
            print(output)
            server_init_response.status = EnviromentSetupStatus.failed
            server_init_response.message = output
        return  server_init_response

    async def create_container_on_remote_server(self,instance_name):
        client = self.__connect_to_client()

        if not client:
            return failed_response(message="Authentication Error Check Your Credentials")

        ## create files and move them
        ## find a safe way to find available ports ona remote server
        response = DPContainerCreationResponseScheme()
        instance_port = await self.port_manager.get_remote_server_open_ports(self._host,self._username,self._password)
        instance_db_port = await self.port_manager.get_remote_server_open_ports(self._host,self._username,self._password)
        configuration_file_manager = ConfigurationFileManager(deployment_engine=self,instance_name=instance_name,instance_port=instance_port,instance_database_port=instance_db_port).move_configuration_files()
        if configuration_file_manager.get("status") != "ok":
            print("something went wrong")

            return failed_response("something went wrong")
        ##exec remote installation command
        remote_dir = configuration_file_manager["folder_remote"]
        stdin, stdout, stderr =client.exec_command(startup_container(remote_dir))
        print(stdout.read().decode())
        print(stderr.read().decode())
        response.message = "Instance created"
        response.status = EnviromentSetupStatus.success
        response.app_port = str(instance_port)
        response.database_port = str(instance_db_port)
        response.instance_artifact_folder_path = remote_dir
        return success_response(data=response)

    def move_module_to_container(self):
        # away to add custom modules to running container pre-jenkins
        pass
    def configure_admin_access(self):
        pass


# class LocalDeploymentEngine(DeploymentEngineBase):
#     pass
# will build a local instance deployer some day lol
