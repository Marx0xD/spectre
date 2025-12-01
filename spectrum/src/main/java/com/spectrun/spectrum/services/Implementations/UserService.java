package com.spectrun.spectrum.services.Implementations;

import com.spectrun.spectrum.DTO.InstanceDto;
import com.spectrun.spectrum.DTO.SubscriptionDTO;
import com.spectrun.spectrum.DTO.UserDTO;
import com.spectrun.spectrum.models.Instances;
import com.spectrun.spectrum.models.Subscriptions;
import com.spectrun.spectrum.models.Users;
import com.spectrun.spectrum.repositories.UserRepsoitory;
import com.spectrun.spectrum.services.user;
import com.spectrun.spectrum.utils.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import com.spectrun.spectrum.utils.mappers.UserMapper;


@Service
public class UserService implements user {
    private UserRepsoitory userRepsoitory;
    private SubscriptionService subscriptionService;
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger(JWTService.class.getName());

    @Autowired
    public UserService(UserRepsoitory userRepsoitory,SubscriptionService subscriptionService) {
        this.userRepsoitory = userRepsoitory;
        this.subscriptionService = subscriptionService;


    }



    @Override
    public List<UserDTO> getUsers() {
        return this.userRepsoitory.findAll().stream().map(UserMapper.INSTANCE::userToUserDto).collect(Collectors.toList());
    }

    @Override
    public UserDTO getUser(long userId) {
        Users user = this.userRepsoitory.getReferenceById(userId);
        return UserMapper.INSTANCE.userToUserDto(user);


    }

    @Override
    public UserDTO getUserByEmail(String email) {
        Users user = this.userRepsoitory.findByEmail(email).orElse(null);
        if(user != null){
            UserDTO subscribtionUser = UserMapper.INSTANCE.userToUserDto(user);
            return  subscribtionUser;
        }
        return  null;
    }

    @Override
    public UserDTO addSubscriptionToUserAccount(long userId, long SubscriptionId) {

        Users user = this.userRepsoitory.findById(userId).orElseThrow(()->new UserNotFoundException("User Not found"));
        Subscriptions subscription = subscriptionService.getSubscriptionById(SubscriptionId);
        if(subscription != null){
            logger.info("subscription not found");
            user.setSubscription(subscription);
            this.userRepsoitory.save(user);
            return  UserMapper.INSTANCE.userToUserDto(user);
        }
        return  null;
    }


}
