import os
import yaml
from time import sleep
from app.schema.instance import Instance, InstanceInformation, InstanceCreationResponse
from app.services.configure_odoo_database import configure_odoo_database
from app.utils.ContainerFileManager import create_new_config_dir
from app.utils.PortManager import PortManager
from app.utils.move_module_to_instance_dir import ModuleManagement
from app.utils.DockerManager import  DockerManager

class ContainerManagementService :
    def __init__(self,instance_name,module_name,module_path,host = None):
        self.instanceName = instance_name
        self.moduleName = module_name
        self.modulePath = module_path

    def create_container(self):
        print(f'.................starting create container: {self.instanceName} ..........')

        instance_data:InstanceInformation = self.create_instance_config(self.instanceName)
        print(instance_data)

        # Move the module to instance directory
        status = ModuleManagement.move_module_to_instance_dir(
        self,
          instance_path= instance_data.custom_addons_path,
          module_dir= self.modulePath
        )

        # Docker installation
        DockerManager().docker_installation(instance_data.configurationFileLocation)

        # Construct URLs
        db_url = f"http://localhost:{instance_data.instanceDbAddress}"
        instance_url = f"http://localhost:{instance_data.instanceAddress}"

        sleep(5)

        config_status = configure_odoo_database(
            instanceUrl=instance_url,
            instance_port=instance_data.instanceAddress,
            odoo_instance_url=instance_url,
            database_name=instance_data.db_name,
            host="localhost"
        )

        # Assuming config_status is a dict or object with those attributes
        container_creation_response:InstanceCreationResponse = InstanceCreationResponse(
            status=config_status.status,
            adminUserName= config_status.adminUserName,
            adminPassword= config_status.adminPassword,
            instanceDbAddress= db_url,
            instanceAddress=instance_url,
            instanceName = self.instanceName,
            configurationFileLocation=instance_data.configurationFileLocation,
            custom_addons_path=""
        )


        return container_creation_response

    def create_instance_config(self,container_instance_name:str)->InstanceInformation:
        instance_config_dir = '/opt/container_configs/'
        instance_information = InstanceInformation()
        instance_port = PortManager().get_open_tcp_ports(self)[1]
        db_port = PortManager().get_open_tcp_ports(self)[1]
        container_port_mapping = [f'{instance_port}:8069']
        db_port_mapping = [f'{db_port}:5432']
        instance_name = container_instance_name
        instance_dir_path = os.path.join(instance_config_dir, instance_name + '/')
        instance_dir = create_new_config_dir(instance_dir_path)
        yaml_file_path = instance_dir + 'docker-compose.yml'

        # Open the existing docker-compose.yml file to edit
        with open(yaml_file_path) as file:
            print('starting file config------')
            base_config = yaml.safe_load(file)

        # Update the existing configuration
        base_config['services']['web']['ports'] = container_port_mapping
        base_config['services']['web']['container_name'] = instance_name
        base_config['services']['db']['container_name'] = instance_name + '_db'
        base_config['services']['db']['ports'] = db_port_mapping

        # Save the updated configuration back to the same file
        with open(yaml_file_path, 'w') as file:
            yaml.safe_dump(base_config, file, default_flow_style=False)

        # Update instance information
        instance_information.db_name = base_config['services']['db']['container_name']
        instance_information.instanceName = instance_name
        instance_information.instanceDbAddress = db_port
        instance_information.configurationFileLocation = yaml_file_path
        instance_information.instanceAddress = instance_port
        instance_information.custom_addons_path = instance_dir_path + "addons"
        print(f"Configuration for instance '{instance_name}' updated at '{yaml_file_path}'")
        return instance_information


