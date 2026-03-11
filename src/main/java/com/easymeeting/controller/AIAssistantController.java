package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.AIMessageDto;
import com.easymeeting.entity.dto.AISuggestionDto;
import com.easymeeting.entity.dto.AISummaryDto;
import com.easymeeting.entity.dto.SmartMeetingSummaryDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.AIAssistantService;
import com.easymeeting.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 助手控制器
 */
@RestController
@RequestMapping("/ai")
public class AIAssistantController extends ABaseController {

    private static final Logger log = LoggerFactory.getLogger(AIAssistantController.class);

    @Autowired
    private AIAssistantService aiAssistantService;

    /**
     * AI 对话
     */
    @PostMapping("/chat")
    @globalInterceptor(checkLogin = true)
    public ResponseVO chat(@RequestBody Map<String, String> params) {
        try {
            TokenUserInfoDto userInfo = getTokenUserInfo();
            if (userInfo == null) {
                return getFailResponseVO("未登录");
            }

            String meetingId = params.get("meetingId");
            String message = params.get("message");

            if (StringTools.isEmpty(meetingId) || StringTools.isEmpty(message)) {
                return getFailResponseVO("meetingId 和 message 不能为空");
            }

            AIMessageDto response = aiAssistantService.chat(meetingId.trim(), userInfo.getUserId(), message.trim());
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return getSuccessResponseVO(response);
            }
            return getFailResponseVO(StringTools.isEmpty(response.getError()) ? "AI 助手处理失败" : response.getError());
        } catch (Exception e) {
            log.error("AI 聊天失败", e);
            return getServerErrorResponseVO("AI 助手暂时无法响应");
        }
    }

    /**
     * 生成会议摘要
     */
    @PostMapping("/summary")
    @globalInterceptor(checkLogin = true)
    public ResponseVO generateSummary(@RequestBody Map<String, String> params) {
        try {
            TokenUserInfoDto userInfo = getTokenUserInfo();
            if (userInfo == null) {
                return getFailResponseVO("未登录");
            }

            String meetingId = params.get("meetingId");
            if (StringTools.isEmpty(meetingId)) {
                return getFailResponseVO("会议ID不能为空");
            }

            AISummaryDto summary = aiAssistantService.generateSummary(meetingId.trim());
            if (summary != null) {
                return getSuccessResponseVO(summary);
            }
            return getFailResponseVO("生成摘要失败");
        } catch (Exception e) {
            log.error("生成会议摘要失败", e);
            return getServerErrorResponseVO("生成摘要失败");
        }
    }

    /**
     * 保存会议发言文本片段
     */
    @PostMapping("/speechSegment")
    @globalInterceptor(checkLogin = true)
    public ResponseVO saveSpeechSegment(@RequestBody Map<String, String> params) {
        try {
            TokenUserInfoDto userInfo = getTokenUserInfo();
            if (userInfo == null) {
                return getFailResponseVO("链櫥褰?");
            }

            String meetingId = params.get("meetingId");
            String speakerName = params.get("speakerName");
            String content = params.get("content");
            if (StringTools.isEmpty(meetingId) || StringTools.isEmpty(content)) {
                return getFailResponseVO("meetingId 鍜 content 涓嶈兘涓虹┖");
            }

            aiAssistantService.saveSpeechSegment(meetingId.trim(), userInfo.getUserId(),
                    StringTools.isEmpty(speakerName) ? userInfo.getNickName() : speakerName.trim(),
                    content.trim());
            return getSuccessResponseVO("OK");
        } catch (Exception e) {
            log.error("保存会议发言片段失败", e);
            return getServerErrorResponseVO("保存会议发言片段失败");
        }
    }

    /**
     * 获取会议建议
     */
    @PostMapping("/suggest")
    @globalInterceptor(checkLogin = true)
    public ResponseVO getSuggestions(@RequestBody Map<String, String> params) {
        try {
            TokenUserInfoDto userInfo = getTokenUserInfo();
            if (userInfo == null) {
                return getFailResponseVO("未登录");
            }

            String meetingId = params.get("meetingId");
            if (StringTools.isEmpty(meetingId)) {
                return getFailResponseVO("会议ID不能为空");
            }

            AISuggestionDto suggestions = aiAssistantService.getSuggestions(meetingId.trim());
            if (suggestions != null) {
                return getSuccessResponseVO(suggestions);
            }
            return getFailResponseVO("获取建议失败");
        } catch (Exception e) {
            log.error("获取会议建议失败", e);
            return getServerErrorResponseVO("获取建议失败");
        }
    }

    /**
     * 测试 AI 连接
     */
    @GetMapping("/test")
    public ResponseVO testConnection() {
        try {
            AIMessageDto response = aiAssistantService.chat("test", "test", "你好");
            return getSuccessResponseVO(response);
        } catch (Exception e) {
            log.error("AI 连接测试失败", e);
            return getServerErrorResponseVO("AI 服务连接失败: " + e.getMessage());
        }
    }

    /**
     * 生成智能会议纪要（结构化）
     */
    @PostMapping("/smartSummary")
    @globalInterceptor(checkLogin = true)
    public ResponseVO generateSmartSummary(@RequestBody Map<String, String> params) {
        try {
            TokenUserInfoDto userInfo = getTokenUserInfo();
            if (userInfo == null) {
                return getFailResponseVO("未登录");
            }

            String meetingId = params.get("meetingId");
            if (StringTools.isEmpty(meetingId)) {
                return getFailResponseVO("会议ID不能为空");
            }

            SmartMeetingSummaryDto summary = aiAssistantService.generateSmartSummary(meetingId.trim());
            if (summary != null) {
                return getSuccessResponseVO(summary);
            }
            return getFailResponseVO("生成智能纪要失败");
        } catch (Exception e) {
            log.error("生成智能会议纪要失败", e);
            return getServerErrorResponseVO("生成智能纪要失败: " + e.getMessage());
        }
    }
}
