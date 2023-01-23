package mk.ukim.finki.wp.kol2022.g3.service.Impl;

import mk.ukim.finki.wp.kol2022.g3.model.ForumUser;
import mk.ukim.finki.wp.kol2022.g3.model.ForumUserType;
import mk.ukim.finki.wp.kol2022.g3.model.Interest;
import mk.ukim.finki.wp.kol2022.g3.model.exceptions.InvalidForumUserIdException;
import mk.ukim.finki.wp.kol2022.g3.model.exceptions.InvalidInterestIdException;
import mk.ukim.finki.wp.kol2022.g3.repository.ForumUserRepository;
import mk.ukim.finki.wp.kol2022.g3.repository.InterestRepository;
import mk.ukim.finki.wp.kol2022.g3.service.ForumUserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ForumUserServiceImpl implements ForumUserService, UserDetailsService {

    private final ForumUserRepository forumUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final InterestRepository interestRepository;

    public ForumUserServiceImpl(ForumUserRepository forumUserRepository, PasswordEncoder passwordEncoder, InterestRepository interestRepository) {
        this.forumUserRepository = forumUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.interestRepository = interestRepository;
    }

    @Override
    public List<ForumUser> listAll() {
        return this.forumUserRepository.findAll();
    }

    @Override
    public ForumUser findById(Long id) {
        return this.forumUserRepository.findById(id).orElseThrow(InvalidForumUserIdException::new);
    }

    @Override
    public ForumUser create(String name, String email, String password, ForumUserType type, List<Long> interestId, LocalDate birthday) {
        List<Interest> interestList = this.interestRepository.findAllById(interestId);
        if(interestList.isEmpty()){
            throw new InvalidInterestIdException();
        }
        ForumUser forumUser = new ForumUser(name, email, passwordEncoder.encode(password), type, interestList, birthday);
        return this.forumUserRepository.save(forumUser);

    }

    @Override
    public ForumUser update(Long id, String name, String email, String password, ForumUserType type,
                            List<Long> interestId, LocalDate birthday) {
        ForumUser forumUser = this.forumUserRepository.findById(id).orElseThrow(InvalidForumUserIdException::new);
        List<Interest> interestList = this.interestRepository.findAllById(interestId);
        if(interestList.isEmpty()){
            throw new InvalidInterestIdException();
        }
        forumUser.setName(name);
        forumUser.setEmail(email);
        forumUser.setPassword(passwordEncoder.encode(password));
        forumUser.setInterests(interestList);
        forumUser.setBirthday(birthday);

        return this.forumUserRepository.save(forumUser);
    }

    @Override
    public ForumUser delete(Long id) {
        ForumUser forumUser = this.forumUserRepository.findById(id).orElseThrow(InvalidForumUserIdException::new);
        this.forumUserRepository.delete(forumUser);
        return forumUser;
    }

    @Override
    public List<ForumUser> filter(Long interestId, Integer age) {
        List<ForumUser> users;
        if(interestId==null&&age==null){
            users = this.forumUserRepository.findAll();
        }
        else if(interestId!=null&&age==null){
            Interest interest = this.interestRepository.getById(interestId);
            users = this.forumUserRepository.findAllByInterests(interest);
        }
        else if (interestId==null&&age!=null){
            users = this.forumUserRepository.findAllByBirthdayBefore(LocalDate.now().minusYears(age));
        }
        else{
            users = this.forumUserRepository.findAllByInterestsAndAndBirthdayBefore(this.interestRepository.getById(interestId), LocalDate.now().minusYears(age));
        }
        return users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ForumUser forumUser = this.forumUserRepository.findByEmail(username);
        if(forumUser==null){
            throw new UsernameNotFoundException(username);
        }

        return new User(
                forumUser.getEmail(),
                forumUser.getPassword(),
                Stream.of(new SimpleGrantedAuthority(String.format("ROLE_%S", forumUser.getType()))).collect(Collectors.toList()));

    }
}
