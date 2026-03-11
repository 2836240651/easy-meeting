package com.easymeeting.websocket;
import com.alibaba.fastjson.JSON;
import com.easymeeting.entity.dto.MeetingExitDto;
import com.easymeeting.entity.dto.MeetingMemberDto;
import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.MeetingMemberStatusEnum;
import com.easymeeting.entity.enums.MessageSend2TypeEnum;
import com.easymeeting.entity.enums.MessageTypeEnum;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.redis.RedisComponent;
import com.easymeeting.utils.JsonUtils;
import com.easymeeting.utils.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@Component
@Slf4j
public class ChannelContextUtils {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChannelContextUtils.class);
    
    @Resource
    private RedisComponent redisComponent;
    
    @Resource
    private com.easymeeting.service.UserContactService userContactService;
    private static final ConcurrentHashMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String,ChannelGroup> MEETING_ROOM_CONTEXT_MAP = new ConcurrentHashMap<>();
    private final UserInfoMapper userInfoMapper;
    //构造函数初始化final字段 因为本身这个类交给spring的component而此时userinfomapper没法初始化只能通过new 或者构造方法将其初始化
    public ChannelContextUtils(UserInfoMapper userInfoMapper) {
        this.userInfoMapper = userInfoMapper;
    }
    //用户连接系统 如果在会议中那么进入会议 一个成员对应一个通道 一个会议对应一个通道组包含多个成员通道; 
    //在创建成员通道的时候需要将成员的id作为通道的属性
    public void addContext(String userId, Channel channel) {
        try{
            log.info("🟢🟢🟢 addContext 被调用: userId={}, channelId={}", userId, channel.id());
            
            String channelId = channel.id().toString();
            AttributeKey<String> attrKey = AttributeKey.valueOf(channelId);
            channel.attr(attrKey).set(userId);
            
            // 检查是否有旧的连接
            Channel oldChannel = USER_CONTEXT_MAP.get(userId);
            if (oldChannel != null && oldChannel != channel) {
                log.info("🟢🟢🟢 用户 {} 有旧连接，关闭旧连接: oldChannelId={}", userId, oldChannel.id());
                try {
                    oldChannel.close();
                } catch (Exception e) {
                    log.warn("关闭旧连接失败: {}", e.getMessage());
                }
            }
            
            USER_CONTEXT_MAP.put(userId, channel);
            log.info("🟢🟢🟢 用户 {} 已添加到 USER_CONTEXT_MAP，当前在线用户数: {}", userId, USER_CONTEXT_MAP.size());
            
            UserInfo userInfo = new UserInfo();
            userInfo.setLastLoginTime(System.currentTimeMillis());
            userInfoMapper.updateByUserId(userInfo,userId);
            log.info("🟢🟢🟢 用户 {} 的 lastLoginTime 已更新", userId);
            
            // 广播在线状态变更给所有联系人
            log.info("🟢🟢🟢 准备广播用户 {} 的在线状态", userId);
            broadcastOnlineStatusChange(userId, 1, System.currentTimeMillis(), null);
            log.info("🟢🟢🟢 用户 {} 的在线状态广播完成", userId);
            
            TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenByUserId(userId);
            if (tokenUserInfoDto != null && tokenUserInfoDto.getCurrentMeetingId() != null){
                String meetingId = tokenUserInfoDto.getCurrentMeetingId();
                addMeetingRoom(userId, meetingId);
                
                // WebSocket连接建立后，发送成员加入通知
                log.info("WebSocket连接已建立，发送成员加入通知: userId={}, meetingId={}", userId, meetingId);
                sendMemberJoinedNotification(userId, meetingId);
            }
        }catch (Exception e) {
            log.error("添加用户上下文失败: {}", e.getMessage(), e);
        }
    }
    public void addMeetingRoom(String userId, String MeetingId) {
        Channel context = USER_CONTEXT_MAP.get(userId);
        if (context==null){
            log.warn("用户 {} 的 WebSocket 连接不存在，无法加入会议房间 {}", userId, MeetingId);
            return;
        }
        
        log.info("用户 {} 加入会议房间 {}", userId, MeetingId);
        
       ChannelGroup group = MEETING_ROOM_CONTEXT_MAP.get(MeetingId);
        if (group==null){
            log.info("会议房间 {} 不存在，创建新房间", MeetingId);
            group =new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            MEETING_ROOM_CONTEXT_MAP.put(MeetingId,group);
        }
        Channel channel = group.find(context.id());
        if (channel==null){
            group.add(context);
            log.info("用户 {} 已添加到会议房间 {}，当前房间成员数: {}", userId, MeetingId, group.size());
        } else {
            log.info("用户 {} 已在会议房间 {} 中", userId, MeetingId);
        }
    }
    public void sendMessage(MessageSendDto messageSendDto){
        // 添加空值检查，防止 NullPointerException
        if (messageSendDto == null) {
            log.warn("messageSendDto 为 null，无法发送消息");
            return;
        }
        
        log.info("🔵🔵🔵 ChannelContextUtils.sendMessage 被调用");
        log.info("🔵🔵🔵 消息类型: {}, 发送者: {}, 接收者: {}, messageSend2Type: {}, meetingId: {}", 
                 messageSendDto.getMessageType(),
                 messageSendDto.getSendUserId(),
                 messageSendDto.getReceiveUserId(),
                 messageSendDto.getMessageSend2Type(),
                 messageSendDto.getMeetingId());
        
        if (messageSendDto.getMessageSend2Type() == null) {
            log.warn("🔵🔵🔵 messageSend2Type 为 null，消息类型: {}, 会议ID: {}, 发送用户: {}, 接收用户: {}", 
                     messageSendDto.getMessageType(), 
                     messageSendDto.getMeetingId(),
                     messageSendDto.getSendUserId(),
                     messageSendDto.getReceiveUserId());
            return;
        }
        
        if (messageSendDto.getMessageSend2Type().equals(MessageSend2TypeEnum.USER.getType())){
            log.info("🔵🔵🔵 路由到 sendMsg2User");
            sendMsg2User(messageSendDto);
        }else {
            log.info("🔵🔵🔵 路由到 sendMsg2Group");
            sendMsg2Group(messageSendDto);
        }
    }
    private void sendMsg2Group(MessageSendDto messageSendDto){
        if (messageSendDto.getMeetingId()==null){
            log.warn("🔵🔵🔵 会议ID为null，无法发送群组消息");
            return;
        }
        ChannelGroup group = MEETING_ROOM_CONTEXT_MAP.get(messageSendDto.getMeetingId());
        if (group==null){
            log.warn("🔵🔵🔵 会议房间 {} 不存在，无法发送群组消息", messageSendDto.getMeetingId());
            log.warn("🔵🔵🔵 当前存在的会议房间: {}", MEETING_ROOM_CONTEXT_MAP.keySet());
            return;
        }
        
        log.info("🔵🔵🔵 向会议房间 {} 发送消息，消息类型: {}, 房间成员数: {}", 
                 messageSendDto.getMeetingId(), 
                 messageSendDto.getMessageType(),
                 group.size());
        
        String messageJson = JSON.toJSONString(messageSendDto);
        log.info("🔵🔵🔵 消息JSON: {}", messageJson);
        
        // 发送消息到所有成员
        group.writeAndFlush(new TextWebSocketFrame(messageJson));
        log.info("🔵🔵🔵 ✅ 消息已发送到会议房间的所有成员");
        
        if(MessageTypeEnum.EXIT_MEETING_ROOM.getType().equals(messageSendDto.getMessageType())){
            log.info("🔵🔵🔵 处理退出会议消息");
            MeetingExitDto exitDto = JsonUtils.convertJson2Obj((String) messageSendDto.getMessageContent(), MeetingExitDto.class);
            log.info("🔵🔵🔵 退出用户ID: {}", exitDto.getExitUserId());
            
            // 从会议房间移除退出用户的Channel
            removeContextFromGroup(exitDto.getExitUserId(),messageSendDto.getMeetingId());
            log.info("🔵🔵🔵 已从会议房间移除用户: {}", exitDto.getExitUserId());
            
            List<MeetingMemberDto> meetingMemberList = redisComponent.getMeetingMemberList(messageSendDto.getMeetingId());
            List<MeetingMemberDto> onlineMemberList = meetingMemberList.stream().filter(item-> MeetingMemberStatusEnum.NORMAL.getStatus().equals(item.getStatus())).collect(Collectors.toList());
            log.info("🔵🔵🔵 当前在线成员数: {}", onlineMemberList.size());
            
            if (onlineMemberList.isEmpty()){
                log.info("🔵🔵🔵 没有在线成员，移除会议房间");
                removeContextGroup(messageSendDto.getMeetingId());
            }
            return;
        }
        if (MessageTypeEnum.FINIS_MEETING.getType().equals(messageSendDto.getMessageType())){
            log.info("🔵🔵🔵 处理结束会议消息");
            List<MeetingMemberDto> meetingMemberList = redisComponent.getMeetingMemberList(messageSendDto.getMeetingId());
            for (MeetingMemberDto memberDto : meetingMemberList) {
                removeContextFromGroup(memberDto.getUserId(),messageSendDto.getMeetingId());
            }
            removeContextGroup(messageSendDto.getMeetingId());
            log.info("🔵🔵🔵 会议房间已清理");
        }
    }
    private void removeContextGroup(String meetingId) {
    MEETING_ROOM_CONTEXT_MAP.remove(meetingId);
    }
    private void removeContextFromGroup(String userId,String meetingId){
        Channel context = USER_CONTEXT_MAP.get(userId);
        if (context==null){
            return;
        }
        ChannelGroup group = MEETING_ROOM_CONTEXT_MAP.get(meetingId);
        if (group!=null){
            group.remove(context);
        }
    }
    private void sendMsg2User(MessageSendDto messageSendDto){
        if (messageSendDto.getReceiveUserId()==null){
            log.warn("接收用户ID为null，无法发送点对点消息");
            return;
        }
        
        log.info("🟢🟢🟢 发送点对点消息: 类型={}, 发送者={}, 接收者={}", 
                 messageSendDto.getMessageType(),
                 messageSendDto.getSendUserId(),
                 messageSendDto.getReceiveUserId());
        
        Channel channel = USER_CONTEXT_MAP.get(messageSendDto.getReceiveUserId());
        if (channel==null){
            log.warn("🟢🟢🟢 接收用户 {} 的WebSocket连接不存在，无法发送消息", messageSendDto.getReceiveUserId());
            log.warn("🟢🟢🟢 当前在线用户列表: {}", USER_CONTEXT_MAP.keySet());
            return;
        }
        
        log.info("🟢🟢🟢 找到接收用户的Channel，准备发送...");
        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(messageSendDto)));
        log.info("🟢🟢🟢 ✅ 消息已发送到用户: {}", messageSendDto.getReceiveUserId());
    }
    private void closeContext(String userId){
        if (StringTools.isEmpty(userId)){
            return;
        }
        Channel channel = USER_CONTEXT_MAP.get(userId);
        USER_CONTEXT_MAP.remove(userId);
        if (channel!=null){
            channel.close();
        }
    }
    
    /**
     * 处理用户离线
     * 更新离线时间并广播状态变更
     */
    public void handleUserOffline(String userId) {
        try {
            if (StringTools.isEmpty(userId)) {
                return;
            }
            
            log.info("处理用户离线: userId={}", userId);
            
            // 更新离线时间
            UserInfo userInfo = new UserInfo();
            Long offTime = System.currentTimeMillis();
            userInfo.setLasgOffTime(offTime);
            userInfoMapper.updateByUserId(userInfo, userId);
            
            // 广播离线状态变更给所有联系人
            broadcastOnlineStatusChange(userId, 0, null, offTime);
            
            log.info("用户 {} 离线处理完成", userId);
        } catch (Exception e) {
            log.error("处理用户离线失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 发送成员加入通知
     * 当用户的WebSocket连接建立并加入会议房间后调用
     */
    private void sendMemberJoinedNotification(String userId, String meetingId) {
        try {
            // 获取新加入的成员信息
            MeetingMemberDto newMember = redisComponent.getMeetingMember(meetingId, userId);
            if (newMember == null) {
                log.warn("无法获取成员信息: userId={}, meetingId={}", userId, meetingId);
                return;
            }
            
            // 获取所有会议成员列表
            List<MeetingMemberDto> meetingMemberList = redisComponent.getMeetingMemberList(meetingId);
            
            // 构造成员加入消息
            com.easymeeting.entity.dto.MeetingJoinDto meetingJoinDto = new com.easymeeting.entity.dto.MeetingJoinDto();
            meetingJoinDto.setNewMember(newMember);
            meetingJoinDto.setMeetingMemberList(meetingMemberList);
            
            MessageSendDto messageSendDto = new MessageSendDto();
            messageSendDto.setMessageType(MessageTypeEnum.ADD_MEETING_ROOM.getType());
            messageSendDto.setMessageContent(meetingJoinDto);
            messageSendDto.setMeetingId(meetingId);
            messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
            
            log.info("发送成员加入通知: userId={}, meetingId={}, 房间成员数={}", userId, meetingId, meetingMemberList.size());
            
            // 直接发送到房间（不通过Redis，因为这是本地操作）
            sendMsg2Group(messageSendDto);
            
        } catch (Exception e) {
            log.error("发送成员加入通知失败: userId={}, meetingId={}, error={}", userId, meetingId, e.getMessage(), e);
        }
    }
    
    /**
     * 广播用户在线状态变更给所有联系人
     * @param userId 用户ID
     * @param onlineStatus 在线状态：1-在线，0-离线
     * @param lastLoginTime 最后登录时间
     * @param lastOffTime 最后离线时间
     */
    private void broadcastOnlineStatusChange(String userId, Integer onlineStatus, Long lastLoginTime, Long lastOffTime) {
        try {
            log.info("广播用户在线状态变更: userId={}, onlineStatus={}", userId, onlineStatus);
            
            // 查询该用户的所有联系人
            com.easymeeting.entity.query.UserContactQuery contactQuery = new com.easymeeting.entity.query.UserContactQuery();
            contactQuery.setContactId(userId);
            contactQuery.setStatus(com.easymeeting.entity.enums.UserContactStatusEnum.FRIEND.getStatus());
            List<com.easymeeting.entity.po.UserContact> contacts = userContactService.findListByParam(contactQuery);
            
            if (contacts == null || contacts.isEmpty()) {
                log.info("用户 {} 没有联系人，无需广播状态变更", userId);
                return;
            }
            
            log.info("用户 {} 有 {} 个联系人，准备广播状态变更", userId, contacts.size());
            
            // 构造在线状态变更消息
            com.easymeeting.entity.dto.UserOnlineStatusDto statusDto = new com.easymeeting.entity.dto.UserOnlineStatusDto();
            statusDto.setUserId(userId);
            statusDto.setOnlineStatus(onlineStatus);
            statusDto.setLastLoginTime(lastLoginTime);
            statusDto.setLastOffTime(lastOffTime);
            
            // 向每个联系人发送状态变更通知
            for (com.easymeeting.entity.po.UserContact contact : contacts) {
                String contactUserId = contact.getUserId();
                
                // 检查联系人是否在线
                Channel contactChannel = USER_CONTEXT_MAP.get(contactUserId);
                if (contactChannel != null) {
                    MessageSendDto messageSendDto = new MessageSendDto();
                    messageSendDto.setMessageType(MessageTypeEnum.USER_ONLINE_STATUS_CHANGE.getType());
                    messageSendDto.setMessageContent(statusDto);
                    messageSendDto.setReceiveUserId(contactUserId);
                    messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
                    
                    sendMsg2User(messageSendDto);
                    log.info("已向联系人 {} 发送用户 {} 的状态变更通知", contactUserId, userId);
                } else {
                    log.debug("联系人 {} 不在线，跳过状态变更通知", contactUserId);
                }
            }
            
        } catch (Exception e) {
            log.error("广播用户在线状态变更失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 根据 Channel 查找对应的 userId
     */
    public String getUserIdByChannel(Channel channel) {
        if (channel == null) {
            return null;
        }
        
        for (Map.Entry<String, Channel> entry : USER_CONTEXT_MAP.entrySet()) {
            if (entry.getValue() != null && entry.getValue().id().equals(channel.id())) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * 从 USER_CONTEXT_MAP 中移除用户
     */
    public void removeContext(String userId) {
        if (StringTools.isEmpty(userId)) {
            return;
        }
        
        Channel channel = USER_CONTEXT_MAP.remove(userId);
        if (channel != null) {
            log.info("用户 {} 已从 USER_CONTEXT_MAP 中移除，当前在线用户数: {}", userId, USER_CONTEXT_MAP.size());
        }
    }
}

