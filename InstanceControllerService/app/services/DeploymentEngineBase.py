from abc import abstractmethod, ABC


class DeploymentEngineBase(ABC):
    @abstractmethod
    def get_ssh_client(self): pass

    @abstractmethod
    def initialize_server(self): pass
