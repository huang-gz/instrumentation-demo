package org.hgz.ins;


import javassist.*;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * @author guozhong.huang
 */
public class MyInstrumentationAgent {

    private static Logger LOGGER = LoggerFactory.getLogger(MyInstrumentationAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In premain method");

//        String className = "org.hgz.ins.MyAtm";

        inst.addTransformer(new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                LOGGER.info("testClassName {}", className);

                if (className.contains("MyAtm")) {
                    LOGGER.info("start initializing");

                    return transformClass(classfileBuffer);
                }
                return classfileBuffer;
            }
        });
    }

    private static byte[] transformClass(byte[] classfileBuffer) {
        LOGGER.info("start transform");
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("withdrawMoney")) {
                    mv = new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/time/LocalDateTime", "now",
                                    "()Ljava/time/LocalDateTime;", false);
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/time/LocalDateTime", "toString",
                                    "()Ljava/lang/String;", false);
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                                    "(Ljava/lang/String;)V", false);
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }



    public static void agentmain(String agentArgs, Instrumentation inst) {

        LOGGER.info("[Agent] In agentmain method");
        String className = "org.hgz.ins.MyAtm";
        transformClass(className,inst);

    }

    private static void transformClass(String className, Instrumentation instrumentation) {
        LOGGER.info("agent transform class is {}", className);
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        // see if we can get the class using forName
        try {
            targetCls = Class.forName(className);
            targetClassLoader = targetCls.getClassLoader();
            transform(targetCls, targetClassLoader, instrumentation);
            return;
        } catch (Exception ex) {

            LOGGER.error("Class [{}] not found with Class.forName");
        }
        // otherwise iterate all loaded classes and find what we want
        for(Class<?> clazz: instrumentation.getAllLoadedClasses()) {
            if(clazz.getName().equals(className)) {
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, instrumentation);
                return;
            }
        }
        throw new RuntimeException("Failed to find class [" + className + "]");
    }

    private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation) {
        LOGGER.info("transform");
        TimeWatcherTransformer dt = new TimeWatcherTransformer(clazz.getName(), classLoader);
        instrumentation.addTransformer(dt, true);
        try {
            LOGGER.info("transformA");

            instrumentation.retransformClasses(clazz);
            LOGGER.info("transformB");

        } catch (Exception ex) {
            throw new RuntimeException("Transform failed for class: [" + clazz.getName() + "]", ex);
        }
    }


    public static class TimeWatcherTransformer implements ClassFileTransformer {

        String targetClassName;

        ClassLoader targetClassLoader;

        public TimeWatcherTransformer(String targetClassName, ClassLoader targetClassLoader) {
            this.targetClassName = targetClassName;
            this.targetClassLoader = targetClassLoader;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            LOGGER.info("target class is {}", targetClassName);
            byte[] byteCode = classfileBuffer;

            String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); //replace . with /
            LOGGER.info("finalTargetClassName {}", finalTargetClassName);
            if (!className.equals(finalTargetClassName)) {
                return byteCode;
            }

            if (className.equals(finalTargetClassName) && loader.equals(targetClassLoader)) {
                LOGGER.info("[Agent] Transforming class DemoApplication");
                try {
                    ClassPool cp = ClassPool.getDefault();
                    CtClass cc = cp.get(targetClassName);
                    CtMethod m = cc.getDeclaredMethod("withdrawMoney");
                    m.addLocalVariable("startTime", CtClass.longType);
                    m.insertBefore("startTime = System.currentTimeMillis();");

                    StringBuilder endBlock = new StringBuilder();

                    m.addLocalVariable("endTime", CtClass.longType);
                    m.addLocalVariable("opTime", CtClass.longType);
                    endBlock.append("endTime = System.currentTimeMillis();");
                    endBlock.append("opTime = (endTime-startTime)/1000;");

                    endBlock.append("LOGGER.info(\"[Application] testExceptionTruncate completed in:\" + opTime + \" seconds!\");");

                    m.insertAfter(endBlock.toString());

                    byteCode = cc.toBytecode();
                    cc.detach();
                } catch (NotFoundException | CannotCompileException | IOException e) {
                    LOGGER.error("Exception", e);
                    throw new RuntimeException(e);
                }
            }
            return byteCode;
        }
    }

}
