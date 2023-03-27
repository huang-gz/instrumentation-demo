package org.hgz.ins;

import com.sun.tools.attach.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author guozhong.huang
 */
public class JavaAttachDemo {

    private static final Logger LOG = LoggerFactory.getLogger(JavaAttachDemo.class);

    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        LOG.info("Starting JavaAttach");

        // 1. 获取 Java 进程列表
        List<VirtualMachineDescriptor> vmdList = VirtualMachine.list();

        // 2. 遍历 Java 进程列表，找到目标进程的 PID
        String targetPid = null;

        for (VirtualMachineDescriptor vmd : vmdList) {
            if (vmd.displayName().contains("application")) { // 按进程名称或类名等来过滤
                targetPid = vmd.id();
                break;
            }
        }

        if (targetPid == null) {
            System.out.println("Target process not found.");
            return;
        }

        // 3. 动态 attach 到目标进程
        VirtualMachine vm = VirtualMachine.attach(targetPid);

        // 4. 加载 agent
        String agentPath = "/Users/hgz/IdeaProjects/instrumentation-demo/agent/target/agent-1.0-SNAPSHOT-jar-with-dependencies.jar"; // 修改为实际的 agent 路径
        vm.loadAgent(agentPath);

        // 5. 执行 agentmain 函数
        String agentArgs = "1,2"; // 修改为实际的 agent 参数
        vm.getAgentProperties().put("agentArgs", agentArgs);
        vm.detach();
    }


}
