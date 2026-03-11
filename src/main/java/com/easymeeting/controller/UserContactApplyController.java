package com.easymeeting.controller;

import java.util.List;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.query.UserContactApplyQuery;
import com.easymeeting.entity.po.UserContactApply;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.UserContactApplyService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 *  Controller
 */
@RestController("userContactApplyController")
@RequestMapping("/userContactApply")
public class UserContactApplyController extends ABaseController{

	@Resource
	private UserContactApplyService userContactApplyService;
	/**
	 * 鏍规嵁鏉′欢鍒嗛〉鏌ヨ
	 */
	@RequestMapping("/loadDataList")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO loadDataList(UserContactApplyQuery query){
		return getSuccessResponseVO(userContactApplyService.findListByPage(query));
	}

	/**
	 * 鏂板
	 */
	@RequestMapping("/add")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO add(UserContactApply bean) {
		userContactApplyService.add(bean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鎵归噺鏂板
	 */
	@RequestMapping("/addBatch")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO addBatch(@RequestBody List<UserContactApply> listBean) {
		userContactApplyService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鎵归噺鏂板/淇敼
	 */
	@RequestMapping("/addOrUpdateBatch")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO addOrUpdateBatch(@RequestBody List<UserContactApply> listBean) {
		userContactApplyService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鏍规嵁ApplyId鏌ヨ瀵硅薄
	 */
	@RequestMapping("/getUserContactApplyByApplyId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO getUserContactApplyByApplyId(Integer applyId) {
		return getSuccessResponseVO(userContactApplyService.getUserContactApplyByApplyId(applyId));
	}

	/**
	 * 鏍规嵁ApplyId淇敼瀵硅薄
	 */
	@RequestMapping("/updateUserContactApplyByApplyId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO updateUserContactApplyByApplyId(UserContactApply bean,Integer applyId) {
		userContactApplyService.updateUserContactApplyByApplyId(bean,applyId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 鏍规嵁ApplyId鍒犻櫎
	 */
	@RequestMapping("/deleteUserContactApplyByApplyId")
	@globalInterceptor(checkAdmin = true)
	public ResponseVO deleteUserContactApplyByApplyId(Integer applyId) {
		userContactApplyService.deleteUserContactApplyByApplyId(applyId);
		return getSuccessResponseVO(null);
	}
}

