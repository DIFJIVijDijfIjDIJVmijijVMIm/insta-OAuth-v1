package com.cos.insta.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.cos.insta.model.Image;
import com.cos.insta.model.User;
import com.cos.insta.repository.FollowRepository;
import com.cos.insta.repository.LikesRepository;
import com.cos.insta.repository.UserRepository;
import com.cos.insta.service.MyUserDetail;

@Controller
public class UserController {
	
	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private BCryptPasswordEncoder encoder;
	
	@Autowired
	private UserRepository mUserRepository;
	
	@Autowired
	private FollowRepository mFollowRepository;
	
	@Autowired
	private LikesRepository mLikesRepository;
	
	@Value("${file.path}")
	private String fileRealPath;
	
	@GetMapping("/auth/login")
	public String authLogin() {
		return "auth/login";
	}
	
	@GetMapping("/auth/join")
	public String authJoin() {
		return "auth/join";
	}
	
	@PostMapping("/user/profileUpload")
	public String userProfileUpload
	(
			@RequestParam("profileImage") MultipartFile file,
			@AuthenticationPrincipal MyUserDetail userDetail
	) throws IOException
	{
		User principal = userDetail.getUser();

		UUID uuid = UUID.randomUUID();
		String uuidFilename = uuid + "_" + file.getOriginalFilename();
		Path filePath = Paths.get(fileRealPath + uuidFilename);
		Files.write(filePath, file.getBytes());
		
		principal.setProfileImage(uuidFilename);
		
		mUserRepository.save(principal);
		return "redirect:/user/"+principal.getId();
	}
	
	@PostMapping("/auth/joinProc")
	public String authJoinProc(User user) {
		String rawPassword = user.getPassword();
		String encPassword = encoder.encode(rawPassword);
		user.setPassword(encPassword);
		log.info("rawPassword : "+rawPassword);
		log.info("encPassword : "+encPassword);

		mUserRepository.save(user);
		
		return "redirect:/auth/login";
	}
	
	@GetMapping("/user/{id}")
	public String profile(
			@PathVariable int id,
			@AuthenticationPrincipal MyUserDetail userDetail,
			Model model) {
		
		/**
		 *   1. imageCount
		 *   2. followerCount
		 *   3. followingCount
		 *   4. User 오브젝트 (Image (likeCount) 컬렉션)
		 *   5. followCheck 팔로우 유무 (1 팔로우, 1이 아니면 언팔로우)
		 */
		
		
		// 4번 임시(수정해야함)
		Optional<User> oUser = mUserRepository.findById(id);
		User user = oUser.get();
		
		//1번
		int imageCount = user.getImages().size();
		model.addAttribute("imageCount", imageCount);
		
		//2번 followCount ( select * from follow where fromUserId = 1 )
		int followCount = mFollowRepository.countByFromUserId(user.getId());
		model.addAttribute("followCount", followCount);
		
		//3번 followerCount ( select count(*) from follower where toUserId = 1 )
		int followerCount = mFollowRepository.countByToUserId(user.getId());
		model.addAttribute("followerCount", followerCount);
		
		//4번 likeCount
		for(Image item : user.getImages()) {
			int likeCount = mLikesRepository.countByImageId(item.getId());
			item.setLikeCount(likeCount);
		}
		
//		Collections.sort(user.getImages());
		model.addAttribute("user", user);
		
		// 5번
		User principal = userDetail.getUser();
		
		int followCheck = mFollowRepository.countByFromUserIdAndToUserId(principal.getId(), id);
		log.info("followCheck : "+followCheck);
		model.addAttribute("followCheck", followCheck);
		
		return "user/profile";
	}
	
	@GetMapping("/user/edit")
	public String userEdit() {
		
		// 해당 id로 Select 하기
		// findByUserInfo() 사용 (만들어야 함)
		
		return "user/profile_edit";
	}
	
	@PutMapping("/user/editProc")
	public String userEditProc(User user) {
		
		mUserRepository.save(user);
		
		return "redirect:/user/"+user.getId();
	}
	
}
