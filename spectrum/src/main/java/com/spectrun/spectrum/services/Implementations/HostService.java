package com.spectrun.spectrum.services.Implementations;

import com.spectrun.spectrum.DTO.HostDto;
import com.spectrun.spectrum.models.Host;
import com.spectrun.spectrum.repositories.HostRepository;
import com.spectrun.spectrum.utils.mappers.HostMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HostService {
    private HostRepository hostRepository;
    private final SymetricEncryptionService encryptionService;


    public HostService(HostRepository hostRepository, SymetricEncryptionService encryptionService) {
        this.hostRepository = hostRepository;
        this.encryptionService = encryptionService;
    }

    public HostDto hostToHostDto(Host host){
        return HostMapper.HOST_MAPPER.hostToHostDto(host);
    }

    public HostDto createHost(Host host){
        String encryptedCred = this.encryptionService.symetricEncryptService(host.getSshPassword());
        host.setSshPassword(encryptedCred);
        Host newHost = this.hostRepository.save(host);
        return  this.hostToHostDto(newHost);
    }
    public Optional<HostDto> findHostById(long id){

       return Optional.ofNullable(this.hostToHostDto(this.hostRepository.findById(id).orElse(null)));

    }
    public  HostDto updateHost(Long id,Host host){
      Optional <Host> targetHost = this.hostRepository.findById(id);
      String encryptedCred = this.encryptionService.symetricEncryptService(host.getSshPassword());
      targetHost.map(hostToUpdate ->{
          hostToUpdate.setHostname(host.getHostname());
          hostToUpdate.setSshPassword(encryptedCred);
          hostToUpdate.setSshUsername(host.getSshUsername());
          return this.hostToHostDto( this.hostRepository.save(hostToUpdate));

      });
      return  null;
    }
    public boolean checkIfHostExists(String host){
        return this.hostRepository.existsByhostname(host);
    }
}
