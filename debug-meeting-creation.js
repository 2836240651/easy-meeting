// 会议创建调试脚本
// 在浏览器控制台中运行此脚本来调试会议创建流程

console.log('🔍 开始调试会议创建流程...');

// 检查当前环境
console.log('📍 当前页面:', window.location.href);
console.log('🔑 Token存在:', !!localStorage.getItem('token'));
console.log('👤 用户信息存在:', !!localStorage.getItem('userInfo'));

// 测试快速会议创建API
async function testQuickMeetingAPI() {
    console.log('🚀 测试快速会议创建API...');
    
    const token = localStorage.getItem('token');
    if (!token) {
        console.error('❌ 没有找到token，请先登录');
        return;
    }
    
    const meetingData = {
        meetingNoType: 1, // 随机生成会议号
        MeetingName: '调试测试会议',
        joinType: 1, // 设置密码
        joinPassword: '12345'
    };
    
    console.log('📤 发送请求数据:', meetingData);
    
    try {
        const response = await fetch('http://localhost:6099/api/meetingInfo/quickMeeting', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'token': token
            },
            body: JSON.stringify(meetingData)
        });
        
        console.log('📥 响应状态:', response.status);
        
        const data = await response.json();
        console.log('📥 响应数据:', data);
        
        if (data.code === 200) {
            const meetingId = data.data;
            console.log('✅ 会议创建成功，会议ID:', meetingId);
            
            // 测试路由跳转
            const meetingUrl = `#/meeting/${meetingId}`;
            console.log('🔗 准备跳转到:', meetingUrl);
            
            // 如果在Vue应用中，可以使用router跳转
            if (window.Vue && window.Vue.router) {
                console.log('🔄 使用Vue Router跳转...');
                window.Vue.router.push(`/meeting/${meetingId}`);
            } else {
                console.log('🔄 使用window.location跳转...');
                window.location.hash = `/meeting/${meetingId}`;
            }
            
            return meetingId;
        } else {
            console.error('❌ 会议创建失败:', data.info);
            return null;
        }
    } catch (error) {
        console.error('❌ 请求失败:', error);
        return null;
    }
}

// 测试路由配置
function testRouteConfig() {
    console.log('🔗 测试路由配置...');
    
    // 检查当前路由
    console.log('当前hash:', window.location.hash);
    
    // 测试不同的路由
    const testRoutes = [
        '#/meeting',
        '#/meeting/test123',
        '#/dashboard'
    ];
    
    testRoutes.forEach(route => {
        console.log(`测试路由: ${route}`);
        // 这里只是打印，不实际跳转
    });
}

// 检查Vue应用状态
function checkVueApp() {
    console.log('🔍 检查Vue应用状态...');
    
    if (typeof Vue !== 'undefined') {
        console.log('✅ Vue已加载');
    } else {
        console.log('❌ Vue未加载');
    }
    
    // 检查Vue应用实例
    const app = document.getElementById('app');
    if (app && app.__vue__) {
        console.log('✅ Vue应用实例存在');
    } else {
        console.log('❌ Vue应用实例不存在');
    }
}

// 主调试函数
async function debugMeetingFlow() {
    console.log('🎯 开始完整调试流程...');
    
    // 1. 检查环境
    checkVueApp();
    testRouteConfig();
    
    // 2. 测试API
    const meetingId = await testQuickMeetingAPI();
    
    if (meetingId) {
        console.log('🎉 调试完成，会议创建成功！');
        console.log('📋 调试总结:');
        console.log(`- 会议ID: ${meetingId}`);
        console.log(`- 会议页面URL: http://localhost:3000/#/meeting/${meetingId}`);
        console.log('- 建议：检查浏览器控制台是否有其他错误信息');
    } else {
        console.log('❌ 调试失败，请检查错误信息');
    }
}

// 导出函数供控制台使用
window.debugMeetingFlow = debugMeetingFlow;
window.testQuickMeetingAPI = testQuickMeetingAPI;
window.checkVueApp = checkVueApp;
window.testRouteConfig = testRouteConfig;

console.log('🔧 调试工具已加载，可以使用以下函数:');
console.log('- debugMeetingFlow(): 完整调试流程');
console.log('- testQuickMeetingAPI(): 测试快速会议API');
console.log('- checkVueApp(): 检查Vue应用状态');
console.log('- testRouteConfig(): 测试路由配置');
console.log('');
console.log('💡 使用方法: 在控制台中输入 debugMeetingFlow() 开始调试');