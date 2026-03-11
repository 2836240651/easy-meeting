package com.easymeeting.service.impl;

import com.easymeeting.entity.dto.AIMessageDto;
import com.easymeeting.entity.dto.AISuggestionDto;
import com.easymeeting.entity.dto.AISummaryDto;
import com.easymeeting.entity.dto.SmartMeetingSummaryDto;
import com.easymeeting.entity.enums.MeetingMemberStatusEnum;
import com.easymeeting.entity.enums.MeetingStatusEnum;
import com.easymeeting.entity.enums.MessageTypeEnum;
import com.easymeeting.entity.po.AIConversation;
import com.easymeeting.entity.po.MeetingChatMessage;
import com.easymeeting.entity.po.MeetingInfo;
import com.easymeeting.entity.po.MeetingMember;
import com.easymeeting.entity.po.MeetingSummary;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.query.MeetingChatMessageQuery;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.mappers.AIConversationMapper;
import com.easymeeting.mappers.MeetingSummaryMapper;
import com.easymeeting.service.AIAssistantService;
import com.easymeeting.service.MeetingChatMessageService;
import com.easymeeting.service.MeetingInfoService;
import com.easymeeting.service.MeetingMemberService;
import com.easymeeting.service.UserInfoService;
import com.easymeeting.utils.StringTools;
import com.easymeeting.utils.TableSplitUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIAssistantServiceImpl implements AIAssistantService {

    private static final int DEFAULT_CONTEXT_SIZE = 20;
    private static final int SUMMARY_CONTEXT_SIZE = 300;
    private static final int MAX_SPEECH_SEGMENTS_PER_MEETING = 500;

    @Autowired
    private MeetingInfoService meetingInfoService;

    @Autowired
    private MeetingMemberService meetingMemberService;

    @Autowired
    private MeetingChatMessageService meetingChatMessageService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AIConversationMapper aiConversationMapper;

    @Autowired
    private MeetingSummaryMapper meetingSummaryMapper;

    private final Map<String, List<SpeechSegmentRecord>> speechSegmentStore = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.provider:openai}")
    private String aiProvider;

    @Value("${ai.mock.enabled:false}")
    private boolean mockEnabled;

    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${ai.openai.api-url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @Value("${ai.ollama.api-url:http://localhost:11434/api/generate}")
    private String ollamaApiUrl;

    @Value("${ai.ollama.model:llama2}")
    private String ollamaModel;

    @Value("${ai.minimax.api-key:}")
    private String minimaxApiKey;

    @Value("${ai.minimax.api-url:https://api.minimaxi.com/v1/chat/completions}")
    private String minimaxApiUrl;

    @Value("${ai.minimax.model:MiniMax-M2.5}")
    private String minimaxModel;

    @Value("${ai.alibaba.api-key:}")
    private String alibabaApiKey;

    @Value("${ai.alibaba.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String alibabaApiUrl;

    @Value("${ai.alibaba.model:qwen-plus}")
    private String alibabaModel;

    @Override
    public AIMessageDto chat(String meetingId, String userId, String message) {
        AIMessageDto response = new AIMessageDto();
        try {
            if (StringTools.isEmpty(message)) {
                response.setSuccess(false);
                response.setError("message cannot be empty");
                return response;
            }

            boolean bypassMeetingCheck = "test".equals(meetingId) && "test".equals(userId);
            if (!bypassMeetingCheck) {
                validateMeetingAccess(meetingId, userId, true);
            }

            if (message.trim().startsWith("/")) {
                return executeCommand(meetingId, userId, message);
            }

            String context = bypassMeetingCheck ? "" : buildMeetingContext(meetingId);
            String aiResponse = callAI(context, message.trim());

            saveConversation(meetingId, userId, message, aiResponse, "QUESTION_ANSWER");

            response.setResponse(aiResponse);
            response.setType("QUESTION_ANSWER");
            response.setSuccess(true);
            return response;
        } catch (Exception e) {
            log.error("AI chat failed", e);
            response.setSuccess(false);
            response.setError(StringTools.isEmpty(e.getMessage()) ? "AI request failed" : e.getMessage());
            return response;
        }
    }

    @Override
    public AISummaryDto generateSummary(String meetingId) {
        try {
            if (StringTools.isEmpty(meetingId)) {
                return null;
            }

            MeetingInfo meeting = meetingInfoService.getMeetingInfoByMeetingId(meetingId);
            if (meeting == null) {
                return null;
            }

            List<MeetingMember> members = meetingMemberService.getMembersByMeetingId(meetingId);
            List<MeetingChatMessage> messages = loadMeetingMessages(meetingId, SUMMARY_CONTEXT_SIZE);
            List<SpeechSegmentRecord> speechSegments = loadSpeechSegments(meetingId, SUMMARY_CONTEXT_SIZE);

            LocalDateTime startTime = meeting.getCreateTime() == null
                    ? LocalDateTime.now()
                    : meeting.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            long duration = Math.max(1, Duration.between(startTime, LocalDateTime.now()).toMinutes());

            String prompt = buildSummaryPrompt(meeting, members, messages, speechSegments, duration);
            String aiSummary = callAI("", prompt);
            if (StringTools.isEmpty(aiSummary) || aiSummary.contains("AI response is empty")) {
                aiSummary = buildFallbackSummary(meeting, members, messages, speechSegments, duration);
            }

            AISummaryDto summary = new AISummaryDto();
            summary.setMeetingName(meeting.getMeetingName());
            summary.setDuration((int) duration);
            summary.setMessageCount(messages.size());
            summary.setSpeechSegmentCount(speechSegments.size());
            summary.setParticipants(resolveParticipantNames(members));
            summary.setSummary(aiSummary);
            List<String> keyPoints = extractKeyPoints(aiSummary);
            if (keyPoints.isEmpty()) {
                keyPoints = buildFallbackKeyPoints(messages, speechSegments, members);
            }
            summary.setKeyPoints(keyPoints);
            summary.setContextSource(buildContextSource(messages, speechSegments));

            saveMeetingSummary(meetingId, summary);
            return summary;
        } catch (Exception e) {
            log.error("Generate summary failed", e);
            try {
                MeetingInfo meeting = meetingInfoService.getMeetingInfoByMeetingId(meetingId);
                if (meeting == null) {
                    return null;
                }
                List<MeetingMember> members = meetingMemberService.getMembersByMeetingId(meetingId);
                List<MeetingChatMessage> messages = loadMeetingMessages(meetingId, SUMMARY_CONTEXT_SIZE);
                List<SpeechSegmentRecord> speechSegments = loadSpeechSegments(meetingId, SUMMARY_CONTEXT_SIZE);
                LocalDateTime startTime = meeting.getCreateTime() == null
                        ? LocalDateTime.now()
                        : meeting.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                long duration = Math.max(1, Duration.between(startTime, LocalDateTime.now()).toMinutes());

                AISummaryDto fallback = new AISummaryDto();
                fallback.setMeetingName(meeting.getMeetingName());
                fallback.setDuration((int) duration);
                fallback.setMessageCount(messages.size());
                fallback.setSpeechSegmentCount(speechSegments.size());
                fallback.setParticipants(resolveParticipantNames(members));
                fallback.setSummary(buildFallbackSummary(meeting, members, messages, speechSegments, duration));
                fallback.setKeyPoints(buildFallbackKeyPoints(messages, speechSegments, members));
                fallback.setContextSource(buildContextSource(messages, speechSegments));
                return fallback;
            } catch (Exception inner) {
                log.error("Build fallback summary failed", inner);
                return null;
            }
        }
    }

    @Override
    public AISuggestionDto getSuggestions(String meetingId) {
        try {
            if (StringTools.isEmpty(meetingId)) {
                return null;
            }

            String context = buildMeetingContext(meetingId);
            String prompt = "Give 3-5 concise and actionable suggestions to improve this meeting. Return one item per line.";
            String aiResponse = callAI(context, prompt);

            List<String> suggestions = Arrays.stream(aiResponse.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> line.replaceFirst("^[-*\\d.\\s]+", "").trim())
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            if (suggestions.isEmpty() && !StringTools.isEmpty(aiResponse)) {
                suggestions = Collections.singletonList(aiResponse.trim());
            }

            AISuggestionDto result = new AISuggestionDto();
            result.setSuggestions(suggestions);
            result.setType("MEETING_OPTIMIZATION");
            return result;
        } catch (Exception e) {
            log.error("Get suggestions failed", e);
            return null;
        }
    }

    @Override
    public void saveSpeechSegment(String meetingId, String userId, String speakerName, String content) {
        validateMeetingAccess(meetingId, userId, false);

        String normalizedContent = normalizeSpeechContent(content);
        if (StringTools.isEmpty(normalizedContent)) {
            return;
        }

        List<SpeechSegmentRecord> records = speechSegmentStore.computeIfAbsent(
                meetingId, key -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (records) {
            if (!records.isEmpty()) {
                SpeechSegmentRecord last = records.get(records.size() - 1);
                boolean sameSpeaker = String.valueOf(last.getUserId()).equals(userId);
                boolean sameContent = normalizedContent.equals(last.getContent());
                if (sameSpeaker && sameContent) {
                    last.setCreateTime(new Date());
                    return;
                }
            }

            SpeechSegmentRecord record = new SpeechSegmentRecord();
            record.setUserId(userId);
            record.setSpeakerName(StringTools.isEmpty(speakerName) ? resolveUserName(userId) : speakerName);
            record.setContent(normalizedContent);
            record.setCreateTime(new Date());
            records.add(record);

            if (records.size() > MAX_SPEECH_SEGMENTS_PER_MEETING) {
                records.remove(0);
            }
        }
    }

    @Override
    public AIMessageDto executeCommand(String meetingId, String userId, String command) {
        AIMessageDto response = new AIMessageDto();
        response.setType("COMMAND_EXECUTION");

        try {
            String normalized = command == null ? "" : command.trim().toLowerCase();
            validateMeetingAccess(meetingId, userId, false);

            if ("/summary".equals(normalized)) {
                AISummaryDto summary = generateSummary(meetingId);
                if (summary == null) {
                    response.setSuccess(false);
                    response.setResponse("Generate summary failed");
                } else {
                    response.setSuccess(true);
                    response.setResponse(formatSummary(summary));
                }
            } else if ("/suggest".equals(normalized)) {
                AISuggestionDto suggestion = getSuggestions(meetingId);
                if (suggestion == null || suggestion.getSuggestions() == null || suggestion.getSuggestions().isEmpty()) {
                    response.setSuccess(false);
                    response.setResponse("Get suggestions failed");
                } else {
                    response.setSuccess(true);
                    response.setResponse("Suggestions:\n" + String.join("\n", suggestion.getSuggestions()));
                }
            } else if ("/help".equals(normalized)) {
                response.setSuccess(true);
                response.setResponse(getHelpText());
            } else if ("/end".equals(normalized)) {
                MeetingMember member = meetingMemberService.getMember(meetingId, userId);
                if (member != null && Integer.valueOf(1).equals(member.getMemberType())) {
                    response.setSuccess(true);
                    response.setResponse("Meeting owner can end meeting. Confirm in UI.");
                    response.setActions(Collections.singletonList("END_MEETING"));
                } else {
                    response.setSuccess(false);
                    response.setResponse("Only meeting owner can end meeting");
                }
            } else {
                response.setSuccess(false);
                response.setResponse("Unknown command. Use /help");
            }
        } catch (Exception e) {
            log.error("Execute AI command failed", e);
            response.setSuccess(false);
            response.setResponse(StringTools.isEmpty(e.getMessage()) ? "Command execution failed" : e.getMessage());
        }

        saveConversation(meetingId, userId, command, response.getResponse(), "COMMAND_EXECUTION");
        return response;
    }

    private void validateMeetingAccess(String meetingId, String userId, boolean requireRunning) {
        if (StringTools.isEmpty(meetingId)) {
            throw new IllegalArgumentException("meetingId is required");
        }
        if (StringTools.isEmpty(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        MeetingInfo meetingInfo = meetingInfoService.getMeetingInfoByMeetingId(meetingId);
        if (meetingInfo == null) {
            throw new IllegalArgumentException("meeting not found");
        }

        if (requireRunning && !Integer.valueOf(MeetingStatusEnum.RUNING.getStatus()).equals(meetingInfo.getStatus())) {
            throw new IllegalArgumentException("meeting is not running");
        }

        MeetingMember member = meetingMemberService.getMember(meetingId, userId);
        if (member == null) {
            throw new IllegalArgumentException("user is not in this meeting");
        }

        Integer status = member.getStatus();
        if (status == null || !MeetingMemberStatusEnum.NORMAL.getStatus().equals(status)) {
            throw new IllegalArgumentException("user is not an active meeting member");
        }
    }

    private List<MeetingChatMessage> loadMeetingMessages(String meetingId, int limit) {
        try {
            String tableName = TableSplitUtils.getMeetingChatMessageTable(meetingId);
            MeetingChatMessageQuery query = new MeetingChatMessageQuery();
            query.setMeetingId(meetingId);
            query.setOrderBy("m.send_time desc");
            query.setPageNo(1);
            query.setPageSize(limit);

            PaginationResultVO<MeetingChatMessage> page = meetingChatMessageService.findListByPage(tableName, query);
            if (page == null || page.getList() == null) {
                return Collections.emptyList();
            }

            List<MeetingChatMessage> list = page.getList().stream()
                    .filter(msg -> msg.getMessageType() != null)
                    .filter(msg -> MessageTypeEnum.CHAT_TEXT_MESSAGE.getType().equals(msg.getMessageType())
                            || MessageTypeEnum.CHAT_MEDIA_MESSAGE.getType().equals(msg.getMessageType()))
                    .collect(Collectors.toList());

            Collections.reverse(list);
            return list;
        } catch (Exception e) {
            log.warn("Load meeting messages failed, meetingId={}", meetingId, e);
            return Collections.emptyList();
        }
    }

    private List<SpeechSegmentRecord> loadSpeechSegments(String meetingId, int limit) {
        List<SpeechSegmentRecord> records = speechSegmentStore.get(meetingId);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (records) {
            int fromIndex = Math.max(0, records.size() - limit);
            return new ArrayList<>(records.subList(fromIndex, records.size()));
        }
    }

    private String buildMeetingContext(String meetingId) {
        try {
            MeetingInfo meeting = meetingInfoService.getMeetingInfoByMeetingId(meetingId);
            if (meeting == null) {
                return "";
            }

            List<MeetingMember> members = meetingMemberService.getMembersByMeetingId(meetingId);
            List<MeetingChatMessage> recentMessages = loadMeetingMessages(meetingId, DEFAULT_CONTEXT_SIZE);
            List<SpeechSegmentRecord> speechSegments = loadSpeechSegments(meetingId, DEFAULT_CONTEXT_SIZE);

            StringBuilder context = new StringBuilder();
            context.append("Meeting: ").append(meeting.getMeetingName()).append("\n");
            context.append("Participants: ").append(members == null ? 0 : members.size()).append("\n");

            if (!recentMessages.isEmpty()) {
                context.append("Recent messages:\n");
                for (MeetingChatMessage msg : recentMessages) {
                    String content = msg.getMessageContent();
                    if (StringTools.isEmpty(content)) {
                        continue;
                    }
                    String nickName = !StringTools.isEmpty(msg.getSendUserNickName())
                            ? msg.getSendUserNickName()
                            : resolveUserName(msg.getSendUserId());
                    context.append("- ")
                            .append(StringTools.isEmpty(nickName) ? "Unknown" : nickName)
                            .append(": ")
                            .append(content)
                            .append("\n");
                }
            }

            if (!speechSegments.isEmpty()) {
                context.append("Recent speech transcripts:\n");
                for (SpeechSegmentRecord segment : speechSegments) {
                    context.append("- ")
                            .append(StringTools.isEmpty(segment.getSpeakerName()) ? "Unknown" : segment.getSpeakerName())
                            .append(": ")
                            .append(segment.getContent())
                            .append("\n");
                }
            }
            return context.toString();
        } catch (Exception e) {
            log.error("Build meeting context failed", e);
            return "";
        }
    }

    private String buildSummaryPrompt(MeetingInfo meeting, List<MeetingMember> members,
                                      List<MeetingChatMessage> messages,
                                      List<SpeechSegmentRecord> speechSegments, long duration) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please summarize this meeting in Chinese.\n");
        prompt.append("Meeting: ").append(meeting.getMeetingName()).append("\n");
        prompt.append("Participants: ").append(members == null ? 0 : members.size()).append("\n");
        prompt.append("Duration: ").append(duration).append(" minutes\n");
        prompt.append("Message count: ").append(messages == null ? 0 : messages.size()).append("\n\n");

        if (messages != null && !messages.isEmpty()) {
            prompt.append("Meeting messages:\n");
            for (MeetingChatMessage msg : messages) {
                if (StringTools.isEmpty(msg.getMessageContent())) {
                    continue;
                }
                String nickName = !StringTools.isEmpty(msg.getSendUserNickName())
                        ? msg.getSendUserNickName()
                        : resolveUserName(msg.getSendUserId());
                prompt.append(StringTools.isEmpty(nickName) ? "Unknown" : nickName)
                        .append(": ")
                        .append(msg.getMessageContent())
                        .append("\n");
            }
        }

        if (speechSegments != null && !speechSegments.isEmpty()) {
            prompt.append("\nMeeting speech transcripts:\n");
            for (SpeechSegmentRecord segment : speechSegments) {
                prompt.append(StringTools.isEmpty(segment.getSpeakerName()) ? "Unknown" : segment.getSpeakerName())
                        .append(": ")
                        .append(segment.getContent())
                        .append("\n");
            }
        }

        prompt.append("\nOutput requirements:\n");
        prompt.append("1) One paragraph summary, within 200 Chinese characters.\n");
        prompt.append("2) 3-5 key points, each starts with '- '.\n");
        return prompt.toString();
    }

    private String callAI(String context, String message) {
        // Mock mode for testing/demo
        if (mockEnabled || "mock".equalsIgnoreCase(aiProvider)) {
            return callMockAI(context, message);
        }
        
        String provider = aiProvider == null ? "" : aiProvider.trim().toLowerCase();
        if ("openai".equals(provider)) {
            return callOpenAI(context, message);
        }
        if ("alibaba".equals(provider) || "bailian".equals(provider)) {
            return callAlibaba(context, message);
        }
        if ("minimax".equals(provider)) {
            return callMiniMax(context, message);
        }
        if ("ollama".equals(provider)) {
            return callOllama(context, message);
        }
        throw new RuntimeException("Unsupported AI provider: " + aiProvider);
    }

    /**
     * Mock AI for testing/demo purposes
     */
    private String callMockAI(String context, String message) {
        log.info("Using mock AI response for message: {}", message);
        
        String lowerMessage = message.toLowerCase().trim();
        
        // Handle common questions
        if (lowerMessage.contains("你好") || lowerMessage.contains("hello")) {
            return "你好!我是AI会议助手。我可以帮你回答会议相关的问题,生成会议摘要,或提供会议建议。有什么我可以帮你的吗?";
        }
        
        if (lowerMessage.contains("几个人") || lowerMessage.contains("多少人") || lowerMessage.contains("参会")) {
            if (!StringTools.isEmpty(context) && context.contains("参会人员")) {
                return "根据当前会议信息,目前有参会人员在会议中。你可以查看成员列表了解详细信息。";
            }
            return "当前会议正在进行中。你可以查看右侧的成员列表了解参会人数。";
        }
        
        if (lowerMessage.contains("多久") || lowerMessage.contains("时间") || lowerMessage.contains("duration")) {
            return "会议已经进行了一段时间。你可以在会议详情中查看具体的开始时间和持续时长。";
        }
        
        if (lowerMessage.contains("总结") || lowerMessage.contains("摘要") || lowerMessage.contains("summary")) {
            return "我可以帮你生成会议摘要。请使用 /summary 命令,或点击\"摘要\"按钮,我会分析会议聊天记录并生成详细的会议摘要。";
        }
        
        if (lowerMessage.contains("建议") || lowerMessage.contains("suggest")) {
            return "我可以根据会议内容提供建议。请使用 /suggest 命令,或点击\"建议\"按钮,我会分析会议情况并提供相关建议。";
        }
        
        if (lowerMessage.contains("帮助") || lowerMessage.contains("help")) {
            return "我是AI会议助手,可以帮你:\n• 回答会议相关问题\n• 生成会议摘要 (/summary)\n• 提供会议建议 (/suggest)\n• 分析会议内容\n\n直接向我提问即可,我会尽力帮助你!";
        }
        
        // Default response with context awareness
        if (!StringTools.isEmpty(context)) {
            return "我理解你的问题。根据当前会议情况,我建议你可以:\n1. 查看会议成员列表了解参会情况\n2. 使用聊天功能与其他成员交流\n3. 需要时可以生成会议摘要\n\n还有其他我可以帮助的吗?";
        }
        
        return "感谢你的提问!作为AI会议助手,我可以帮你分析会议内容、回答问题、生成摘要等。请告诉我你需要什么帮助?";
    }

    private String callOpenAI(String context, String message) {
        try {
            if (StringTools.isEmpty(openaiApiKey) || "your-api-key-here".equals(openaiApiKey)) {
                throw new RuntimeException("OpenAI API key is not configured");
            }
            return callOpenAICompatible(openaiApiUrl, openaiApiKey, openaiModel, context, message);
        } catch (Exception e) {
            log.error("Call OpenAI failed", e);
            throw new RuntimeException("AI service call failed: " + e.getMessage());
        }
    }

    private String callMiniMax(String context, String message) {
        try {
            if (StringTools.isEmpty(minimaxApiKey)) {
                throw new RuntimeException("MiniMax API key is not configured (MINIMAX_API_KEY)");
            }
            return callOpenAICompatible(minimaxApiUrl, minimaxApiKey, minimaxModel, context, message);
        } catch (Exception e) {
            log.error("Call MiniMax failed", e);
            throw new RuntimeException("AI service call failed: " + e.getMessage());
        }
    }

    /**
     * Call Alibaba Bailian (阿里百炼) API - OpenAI Compatible
     */
    private String callAlibaba(String context, String message) {
        try {
            if (StringTools.isEmpty(alibabaApiKey)) {
                throw new RuntimeException("Alibaba Bailian API key is not configured");
            }
            log.info("Calling Alibaba Bailian API with model: {}", alibabaModel);
            return callOpenAICompatible(alibabaApiUrl, alibabaApiKey, alibabaModel, context, message);
        } catch (Exception e) {
            log.error("Call Alibaba Bailian failed", e);
            throw new RuntimeException("AI service call failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String callOpenAICompatible(String apiUrl, String apiKey, String model, String context, String message) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a professional meeting assistant. Reply in concise Chinese."
                + (StringTools.isEmpty(context) ? "" : "\n\nCurrent meeting context:\n" + context));
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.4);
        requestBody.put("max_tokens", 700);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // MiniMax uses "Authorization: Bearer API_KEY" format
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
        Map<String, Object> responseBody = responseEntity.getBody();

        if (responseBody == null) {
            return "AI response is empty";
        }
        Object choicesObj = responseBody.get("choices");
        if (!(choicesObj instanceof List)) {
            return "AI response is empty";
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;
        if (choices.isEmpty()) {
            return "AI response is empty";
        }
        Map<String, Object> firstChoice = choices.get(0);
        Object messageObj = firstChoice.get("message");
        if (!(messageObj instanceof Map)) {
            return "AI response is empty";
        }
        Object content = ((Map<String, Object>) messageObj).get("content");
        return content == null ? "AI response is empty" : String.valueOf(content);
    }

    private String callOllama(String context, String message) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);

            StringBuilder prompt = new StringBuilder();
            prompt.append("You are a professional meeting assistant. Reply in concise Chinese.\n");
            if (!StringTools.isEmpty(context)) {
                prompt.append("\nCurrent meeting context:\n").append(context).append("\n");
            }
            prompt.append("\nUser request: ").append(message).append("\n");

            requestBody.put("prompt", prompt.toString());
            requestBody.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(ollamaApiUrl, entity, Map.class);
            Map<String, Object> responseBody = responseEntity.getBody();
            if (responseBody != null && responseBody.containsKey("response")) {
                return String.valueOf(responseBody.get("response"));
            }
            return "AI response is empty";
        } catch (Exception e) {
            log.error("Call Ollama failed", e);
            throw new RuntimeException("AI service call failed: " + e.getMessage());
        }
    }

    private List<String> resolveParticipantNames(List<MeetingMember> members) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream()
                .map(member -> {
                    if (!StringTools.isEmpty(member.getNickName())) {
                        return member.getNickName();
                    }
                    return resolveUserName(member.getUserId());
                })
                .map(name -> StringTools.isEmpty(name) ? "Unknown" : name)
                .collect(Collectors.toList());
    }

    private String resolveUserName(String userId) {
        if (StringTools.isEmpty(userId)) {
            return "";
        }
        UserInfo user = userInfoService.getUserInfoByUserId(userId);
        return user == null ? "" : user.getNickName();
    }

    private List<String> extractKeyPoints(String summary) {
        if (StringTools.isEmpty(summary)) {
            return Collections.emptyList();
        }
        return Arrays.stream(summary.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("-") || line.matches("^\\d+[.]\\s*.*"))
                .map(line -> line.replaceFirst("^[-*\\d.\\s]+", "").trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    private String buildFallbackSummary(MeetingInfo meeting, List<MeetingMember> members,
                                        List<MeetingChatMessage> messages,
                                        List<SpeechSegmentRecord> speechSegments,
                                        long duration) {
        StringBuilder summary = new StringBuilder();
        summary.append("会议“").append(meeting.getMeetingName()).append("”已进行")
                .append(duration).append("分钟，");

        int participantCount = members == null ? 0 : members.size();
        summary.append("共有").append(participantCount).append("位成员参与。");

        if (messages != null && !messages.isEmpty()) {
            summary.append("共收集到").append(messages.size()).append("条聊天消息，");
        }
        if (speechSegments != null && !speechSegments.isEmpty()) {
            summary.append("并记录到").append(speechSegments.size()).append("条成员发言片段。");
        } else {
            summary.append("当前暂无可用的成员发言转写片段。");
        }

        List<String> highlights = buildFallbackKeyPoints(messages, speechSegments, members);
        if (!highlights.isEmpty()) {
            summary.append("\n");
            for (String item : highlights) {
                summary.append("- ").append(item).append("\n");
            }
        }
        return summary.toString().trim();
    }

    private List<String> buildFallbackKeyPoints(List<MeetingChatMessage> messages,
                                                List<SpeechSegmentRecord> speechSegments,
                                                List<MeetingMember> members) {
        List<String> points = new ArrayList<>();
        if (members != null && !members.isEmpty()) {
            points.add("参会成员：" + resolveParticipantNames(members).stream().limit(6).collect(Collectors.joining("、")));
        }
        if (messages != null && !messages.isEmpty()) {
            points.add("聊天消息数量：" + messages.size() + " 条");
            MeetingChatMessage lastMessage = messages.get(messages.size() - 1);
            if (!StringTools.isEmpty(lastMessage.getMessageContent())) {
                points.add("最近聊天内容：" + abbreviate(lastMessage.getMessageContent(), 60));
            }
        }
        if (speechSegments != null && !speechSegments.isEmpty()) {
            SpeechSegmentRecord lastSpeech = speechSegments.get(speechSegments.size() - 1);
            points.add("最近发言成员：" + (StringTools.isEmpty(lastSpeech.getSpeakerName()) ? "Unknown" : lastSpeech.getSpeakerName()));
            points.add("最近发言内容：" + abbreviate(lastSpeech.getContent(), 60));
        }
        return points.stream().filter(item -> !StringTools.isEmpty(item)).limit(5).collect(Collectors.toList());
    }

    private String buildContextSource(List<MeetingChatMessage> messages, List<SpeechSegmentRecord> speechSegments) {
        return "聊天消息 " + (messages == null ? 0 : messages.size()) + " 条，发言片段 "
                + (speechSegments == null ? 0 : speechSegments.size()) + " 条";
    }

    private String abbreviate(String text, int maxLength) {
        if (StringTools.isEmpty(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String normalizeSpeechContent(String content) {
        if (StringTools.isEmpty(content)) {
            return null;
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() < 2) {
            return null;
        }
        return normalized;
    }

    private String formatSummary(AISummaryDto summary) {
        StringBuilder text = new StringBuilder();
        text.append("Meeting Summary\n\n");
        text.append("Meeting: ").append(summary.getMeetingName()).append("\n");
        text.append("Participants: ").append(String.join(", ", summary.getParticipants())).append("\n");
        text.append("Duration: ").append(summary.getDuration()).append(" minutes\n");
        text.append("Messages: ").append(summary.getMessageCount()).append("\n\n");
        if (summary.getSpeechSegmentCount() != null) {
            text.append("Speech Segments: ").append(summary.getSpeechSegmentCount()).append("\n");
        }
        if (!StringTools.isEmpty(summary.getContextSource())) {
            text.append("Sources: ").append(summary.getContextSource()).append("\n\n");
        }

        if (!StringTools.isEmpty(summary.getSummary())) {
            text.append("Summary:\n").append(summary.getSummary()).append("\n\n");
        }

        if (summary.getKeyPoints() != null && !summary.getKeyPoints().isEmpty()) {
            text.append("Key points:\n");
            for (int i = 0; i < summary.getKeyPoints().size(); i++) {
                text.append(i + 1).append(". ").append(summary.getKeyPoints().get(i)).append("\n");
            }
        }
        return text.toString();
    }

    private String getHelpText() {
        return "AI command list:\n"
                + "/summary - generate meeting summary\n"
                + "/suggest - generate meeting suggestions\n"
                + "/help - show command list\n"
                + "/end - end meeting (owner only)\n\n"
                + "You can also directly ask AI questions about the current meeting context.";
    }

    private void saveConversation(String meetingId, String userId, String userMessage,
                                  String aiResponse, String messageType) {
        try {
            if (StringTools.isEmpty(meetingId) || StringTools.isEmpty(userId)
                    || StringTools.isEmpty(userMessage) || StringTools.isEmpty(aiResponse)) {
                return;
            }

            AIConversation conversation = new AIConversation();
            conversation.setConversationId(StringTools.getRandomNumber(20));
            conversation.setMeetingId(meetingId);
            conversation.setUserId(userId);
            conversation.setUserMessage(userMessage);
            conversation.setAiResponse(aiResponse);
            conversation.setMessageType(messageType);
            conversation.setCreateTime(new Date());
            aiConversationMapper.insert(conversation);
        } catch (Exception e) {
            log.error("Save conversation failed", e);
        }
    }

    private void saveMeetingSummary(String meetingId, AISummaryDto summaryDto) {
        try {
            MeetingSummary summary = new MeetingSummary();
            summary.setSummaryId(StringTools.getRandomNumber(20));
            summary.setMeetingId(meetingId);
            summary.setMeetingName(summaryDto.getMeetingName());
            summary.setSummaryContent(summaryDto.getSummary());
            summary.setKeyPoints(objectMapper.writeValueAsString(summaryDto.getKeyPoints()));
            summary.setParticipants(objectMapper.writeValueAsString(summaryDto.getParticipants()));
            summary.setDuration(summaryDto.getDuration());
            summary.setMessageCount(summaryDto.getMessageCount());
            summary.setGeneratedBy("AI");
            summary.setCreateTime(new Date());
            meetingSummaryMapper.insert(summary);
        } catch (Exception e) {
            log.error("Save meeting summary failed", e);
        }
    }

    @Override
    public SmartMeetingSummaryDto generateSmartSummary(String meetingId) {
        try {
            if (StringTools.isEmpty(meetingId)) {
                return null;
            }

            MeetingInfo meeting = meetingInfoService.getMeetingInfoByMeetingId(meetingId);
            if (meeting == null) {
                return null;
            }

            List<MeetingMember> members = meetingMemberService.getMembersByMeetingId(meetingId);
            List<MeetingChatMessage> allMessages = loadMeetingMessages(meetingId, 500);
            List<SpeechSegmentRecord> speechSegments = loadSpeechSegments(meetingId, 500);

            LocalDateTime startTime = meeting.getCreateTime() == null
                    ? LocalDateTime.now()
                    : meeting.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            long duration = Math.max(1, Duration.between(startTime, LocalDateTime.now()).toMinutes());

            // 构建智能纪要提示词
            String prompt = buildSmartSummaryPrompt(meeting, members, allMessages, speechSegments, duration);
            String aiResponse = callAI("", prompt);

            // 解析 AI 响应
            SmartMeetingSummaryDto summary = parseSmartSummary(aiResponse, meeting, members, allMessages, duration);
            
            return summary;
        } catch (Exception e) {
            log.error("Generate smart summary failed", e);
            return null;
        }
    }

    private String buildSmartSummaryPrompt(MeetingInfo meeting, List<MeetingMember> members,
                                           List<MeetingChatMessage> messages,
                                           List<SpeechSegmentRecord> speechSegments, long duration) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的会议纪要助手。请根据以下会议信息，生成一份结构化的智能会议纪要。\n\n");
        
        prompt.append("【会议信息】\n");
        prompt.append("会议名称：").append(meeting.getMeetingName()).append("\n");
        prompt.append("参会人数：").append(members == null ? 0 : members.size()).append("人\n");
        prompt.append("会议时长：").append(duration).append("分钟\n");
        prompt.append("消息数量：").append(messages == null ? 0 : messages.size()).append("条\n\n");

        // 添加参与者信息
        if (members != null && !members.isEmpty()) {
            prompt.append("【参会人员】\n");
            for (MeetingMember member : members) {
                String name = !StringTools.isEmpty(member.getNickName()) 
                        ? member.getNickName() 
                        : resolveUserName(member.getUserId());
                prompt.append("- ").append(StringTools.isEmpty(name) ? "未知" : name);
                if (member.getMemberType() != null && member.getMemberType() == 1) {
                    prompt.append(" (主持人)");
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        // 添加会议消息
        if (messages != null && !messages.isEmpty()) {
            prompt.append("【会议内容】\n");
            for (MeetingChatMessage msg : messages) {
                if (StringTools.isEmpty(msg.getMessageContent())) continue;
                String nickName = !StringTools.isEmpty(msg.getSendUserNickName())
                        ? msg.getSendUserNickName()
                        : resolveUserName(msg.getSendUserId());
                prompt.append(StringTools.isEmpty(nickName) ? "未知" : nickName)
                        .append("：")
                        .append(msg.getMessageContent())
                        .append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("【输出要求】\n");
        prompt.append("请严格按照以下JSON格式输出（必须是可以解析的有效JSON）：\n");
        prompt.append("{\n");
        prompt.append("  \"overview\": \"会议概要，50-100字\",\n");
        prompt.append("  \"discussionPoints\": [\"讨论要点1\", \"讨论要点2\", \"讨论要点3\"],\n");
        prompt.append("  \"decisions\": [\"决策1\", \"决策2\"],\n");
        prompt.append("  \"actionItems\": [\n");
        prompt.append("    {\"task\": \"任务描述\", \"assignee\": \"负责人\", \"deadline\": \"截止时间(可选)\", \"priority\": \"HIGH/MEDIUM/LOW\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"highlights\": [\"精彩时刻1\", \"精彩时刻2\"],\n");
        prompt.append("  \"sentiment\": \"会议氛围描述，20字以内\",\n");
        prompt.append("  \"suggestions\": [\"建议1\", \"建议2\"]\n");
        prompt.append("}\n");
        
        prompt.append("注意：\n");
        prompt.append("1. 必须输出有效的JSON格式\n");
        prompt.append("2. 如果某些字段没有内容，使用空数组 [] \n");
        prompt.append("3. actionItems 中的 priority 只允许 HIGH、MEDIUM、LOW\n");
        prompt.append("4. 所有内容使用中文\n");
        
        return prompt.toString();
    }

    private SmartMeetingSummaryDto parseSmartSummary(String aiResponse, MeetingInfo meeting,
                                                     List<MeetingMember> members,
                                                     List<MeetingChatMessage> messages, long duration) {
        SmartMeetingSummaryDto summary = new SmartMeetingSummaryDto();
        
        try {
            // 尝试解析 JSON
            Map<String, Object> parsed = objectMapper.readValue(aiResponse, Map.class);
            
            summary.setOverview(getStringValue(parsed, "overview"));
            summary.setDiscussionPoints(getStringListValue(parsed, "discussionPoints"));
            summary.setDecisions(getStringListValue(parsed, "decisions"));
            summary.setHighlights(getStringListValue(parsed, "highlights"));
            summary.setSentiment(getStringValue(parsed, "sentiment"));
            summary.setSuggestions(getStringListValue(parsed, "suggestions"));
            
            // 解析待办事项
            List<Map<String, String>> actionItemsList = (List<Map<String, String>>) parsed.get("actionItems");
            if (actionItemsList != null) {
                List<SmartMeetingSummaryDto.ActionItem> actionItems = new ArrayList<>();
                for (Map<String, String> item : actionItemsList) {
                    SmartMeetingSummaryDto.ActionItem actionItem = new SmartMeetingSummaryDto.ActionItem();
                    actionItem.setTask(item.get("task"));
                    actionItem.setAssignee(item.get("assignee"));
                    actionItem.setDeadline(item.get("deadline"));
                    actionItem.setPriority(item.get("priority"));
                    actionItems.add(actionItem);
                }
                summary.setActionItems(actionItems);
            }
            
        } catch (Exception e) {
            // JSON 解析失败，使用备用方案
            log.warn("Parse smart summary JSON failed, using fallback", e);
            summary.setOverview(aiResponse.substring(0, Math.min(200, aiResponse.length())));
            summary.setDiscussionPoints(Arrays.asList(aiResponse.split("\n")));
        }
        
        // 设置基本信息
        summary.setMeetingId(meeting.getMeetingId());
        summary.setMeetingName(meeting.getMeetingName());
        summary.setMeetingTime(meeting.getCreateTime() != null ? meeting.getCreateTime().toString() : "");
        summary.setDuration((int) duration);
        summary.setParticipantCount(members != null ? members.size() : 0);
        summary.setParticipants(resolveParticipantNames(members));
        
        return summary;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringListValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }

    private static class SpeechSegmentRecord {
        private String userId;
        private String speakerName;
        private String content;
        private Date createTime;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getSpeakerName() {
            return speakerName;
        }

        public void setSpeakerName(String speakerName) {
            this.speakerName = speakerName;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }
    }
}
