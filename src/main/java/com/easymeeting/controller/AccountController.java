package com.easymeeting.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.Random;
import javax.imageio.ImageIO;
import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.SystemSettingDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.dto.LoginDto;
import com.easymeeting.entity.dto.RegisterDto;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.vo.CheckCodeVo;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.entity.vo.UserInfoVo;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.redis.RedisComponent;
import com.easymeeting.service.UserInfoService;
import com.easymeeting.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *  Controller
 */
@RestController("userInfoController")
@RequestMapping("/account")
@Validated
@Slf4j
public class AccountController extends ABaseController{

	@Resource
	private UserInfoService userInfoService;
	@Resource
	private RedisComponent redisComponent;


	@globalInterceptor(checkLogin = false,checkAdmin = false)
	@RequestMapping({"/checkCode", "/captcha"})
	public ResponseVO checkcode() {
		log.info("获取验证码！");
		// 使用Java内置API生成验证码，避免依赖Nashorn
		int width = 100;
		int height = 42;
		int codeLength = 4;
		
		// 创建图像
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		
		// 设置背景
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		
		// 设置边框
		g.setColor(Color.GRAY);
		g.drawRect(0, 0, width - 1, height - 1);
		
		// 生成随机验证码
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuilder codeBuilder = new StringBuilder();
		
		// 生成验证码字符
		for (int i = 0; i < codeLength; i++) {
			int index = random.nextInt(chars.length());
			char c = chars.charAt(index);
			codeBuilder.append(c);
			
			// 绘制字符
			g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
			g.setFont(new Font("Arial", Font.BOLD, 20));
			g.drawString(String.valueOf(c), 20 + i * 20, 28);
		}
		
		String code = codeBuilder.toString();
		
		// 添加干扰线
		for (int i = 0; i < 5; i++) {
			g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
			g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
		}
		
		// 关闭图形上下文
		g.dispose();
		
		// 转换为Base64
		String checkcodeBase64 = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageBytes = baos.toByteArray();
			checkcodeBase64 = Base64.getEncoder().encodeToString(imageBytes);
			// 移除Base64前缀，只返回纯数据部分
		} catch (IOException e) {
			log.error("生成验证码失败", e);
			return getServerErrorResponseVO(null);
		}
		
		log.info("code:{}", code);
		String checkCodeKey = redisComponent.saveCheckCode(code);
		CheckCodeVo checkCodeVo = new CheckCodeVo();
		checkCodeVo.setCheckCode(checkcodeBase64);
		checkCodeVo.setCheckCodeKey(checkCodeKey);
		return getSuccessResponseVO(checkCodeVo);
	}

	@RequestMapping("/login")
	public ResponseVO login(@Validated @RequestBody LoginDto loginDto) {

		try{
			if (!loginDto.getCheckCode().equalsIgnoreCase(redisComponent.getcheckCode(loginDto.getCheckCodeKey()))) {
				throw new BusinessException("图片验证码不正确");
			}
			UserInfoVo userInfoVo = this.userInfoService.login(loginDto.getEmail(), loginDto.getPassword());
			return getSuccessResponseVO(userInfoVo);
		}finally {
			redisComponent.cleanCheckCode(loginDto.getCheckCodeKey());
		}


	}
	@RequestMapping("/register")
	public ResponseVO register(@Validated @RequestBody RegisterDto registerDto) {
	try{
		if (!registerDto.getCheckCode().equalsIgnoreCase(redisComponent.getcheckCode(registerDto.getCheckCodeKey()))) {
		throw new BusinessException("图片验证码不正确");
		}
		this.userInfoService.register(registerDto.getEmail(), registerDto.getNickName(), registerDto.getPassword());
		return getSuccessResponseVO("注册成功！！");
	}finally {
		redisComponent.cleanCheckCode(registerDto.getCheckCodeKey());
	}
	}


	

	@RequestMapping("/logout")
	public ResponseVO logout() {
		       return null;
	}

	@RequestMapping("/loadSystemSetting")
	public ResponseVO loadSystemSetting() {
		SystemSettingDto settingDto = redisComponent.getSystemSetting();
		return getSuccessResponseVO(settingDto);
	}


	/*
	* 更新个人信息通过当前token的userid进行修改
	*
	*
	* */
	@globalInterceptor(checkLogin = true)
	@RequestMapping("/updateUserInfo")
	public ResponseVO updateUserInfo(MultipartFile avatar, @NotEmpty String nickName, @NotNull Integer sex) throws IOException {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserInfo userInfo = new UserInfo();
		userInfo.setSex(sex);
		userInfo.setNickName(nickName);
		this.userInfoService.updateUserInfoByUserId(userInfo,tokenUserInfo.getUserId(),avatar);
		return getSuccessResponseVO(null);
	}
	@RequestMapping("/updatePassword")
	@globalInterceptor
	public ResponseVO updateUserInfo(@NotEmpty @Size(max = 32) String password) {
		TokenUserInfoDto tokenUserInfo = getTokenUserInfo();
		UserInfo userInfo = new UserInfo();
		userInfo.setPassword(StringTools.encodebyMd5(password));
		this.userInfoService.updatePassword(userInfo,tokenUserInfo.getUserId());
		return getSuccessResponseVO(null);
	}

}