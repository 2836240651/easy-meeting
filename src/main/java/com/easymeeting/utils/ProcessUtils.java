package com.easymeeting.utils;

import com.easymeeting.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    private static final String osName = System.getProperty("os.name").toLowerCase();

    public static String executeCommand(String cmd) throws BusinessException {
        if (StringTools.isEmpty(cmd)) {
            return null;
        }

        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            //判断操作系统
            if (osName.contains("win")) {
                process = Runtime.getRuntime().exec(cmd);
            } else {
                process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
            }
            // 执行ffmpeg指令
            // 取出输出流和错误流的信息
            // 注意：必须要取出ffmpeg在执行命令过程中产生的输出信息，
           // 如果不取的话当输出流信息填满jvm存储输出留信息的缓冲区时，线程就回阻塞住，导致程序无法正常执行
            PrintStream errorStream = new PrintStream(process.getErrorStream());
            PrintStream inputStream = new PrintStream(process.getInputStream());
            errorStream.start();
            inputStream.start();
            // 等待ffmpeg命令执行完
            process.waitFor();
            // 获取执行结果字符串
            String result = errorStream.stringBuffer.append(inputStream.stringBuffer + "\n").toString();
            // 输出执行的命令信息
          logger.info("执行命令{}结果{}", cmd, result);
            return result;
        } catch (Exception e) {
            logger.error("执行命令失败cmd{}失败:{} ", cmd, e.getMessage());
            throw new BusinessException("视频转换失败");
        } finally {
            if (null != process) {
                ProcessKiller ffmpegKiller = new ProcessKiller(process);
                runtime.addShutdownHook(ffmpegKiller);
            }
        }
    }

    /**
     * 在程序退出前结束已有的FFmpeg进程
     */
    private static class ProcessKiller extends Thread {
        private Process process;

        public ProcessKiller(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            this.process.destroy();
        }
    }


    /**
     * 用于取出ffmpeg线程执行过程中产生的各种输出和错误流的信息
     */
    static class PrintStream extends Thread {
        //初始化输入流和缓存区以及输出流字符串  
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        StringBuffer stringBuffer = new StringBuffer();

        //初始化输入流
        public PrintStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }
        //重写run方法，用于取出ffmpeg线程执行过程中产生的各种输出和错误流的信息
        //如果输入流为空，则返回
        //如果输入流不为空，则创建一个BufferedReader对象，用于读取输入流中的数据
        //如果读取到数据，则将数据添加到stringBuffer中
        //如果读取数时报错，则打印错误信息
        //如果读取完成后，则关闭输入流和BufferedReader对象
        //如果关闭时报错，则打印错误信息
        //如果关闭成功，则打印成功信息
        @Override
        public void run() {
            try {
                if (null == inputStream) {
                    return;
                }
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }
            } catch (Exception e) {
                logger.error("读取输入流出错了！错误信息：" + e.getMessage());
            } finally {
                try {
                    if (null != bufferedReader) {
                        bufferedReader.close();
                    }
                    if (null != inputStream) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    logger.error("调用PrintStream读取输出流后，关闭流时出错！");
                }
            }
        }
    }
}