import xmlrpc.client
import socket
import time
import odoorpc

from app.schema.instance import Instance


# Create a custom Transport class with a timeout
class TimeoutTransport(xmlrpc.client.Transport):
    def __init__(self, timeout=None):
        super().__init__()
        self.timeout = timeout

    def make_connection(self, host):
        connection = super().make_connection(host)
        if self.timeout:
            connection.timeout = self.timeout
        return connection

    def single_request(self, host, handler, request_body, verbose):
        try:
            return super().single_request(host, handler, request_body, verbose)
        except socket.timeout:
            raise TimeoutError("The XML-RPC request timed out")


# Function to configure Odoo database with retry and sleep logic
def configure_odoo_database(instanceUrl,instance_port,odoo_instance_url,database_name, host="localhost",timeout=120, max_retries=3)->Instance:
    url = f'{odoo_instance_url}/xmlrpc/2/db'
    config_response:Instance = {}
    print(instanceUrl,instance_port)
    odoo = odoorpc.ODOO(host, port=int(instance_port), timeout=timeout)
    admin_password = "superadminsaas"
    transport = TimeoutTransport(timeout=timeout)
    attempt = 0
    while attempt < max_retries:
        try:
            # Create the database with the XML-RPC call
            odoo.db.create(
                admin_password,          # Master password for Odoo server
                database_name,           # Name of the new database
                False,                   # Load demo data (False means do not load demo data)
                'en_US',                 # Language code
                admin_password           # Admin password for the new database
            )
            print(f"Database '{database_name}' created successfully.")
            config_response.instanceCreationStatus = True
            config_response.adminUserName = "admin"
            config_response.adminPassword = admin_password
            return config_response

        except TimeoutError as te:
            print(f"Timeout Error: {te}")
            attempt += 1
            if attempt < max_retries:
                print(f"Retrying in 5 seconds... (Attempt {attempt} of {max_retries})")
                time.sleep(5)  # Sleep for 5 seconds before retrying

        except Exception as e:
            print(f"Error creating database: {e}")
            config_response["status"] = False
            return config_response

    print("Max retries exceeded. Could not create the database.")
    return False


