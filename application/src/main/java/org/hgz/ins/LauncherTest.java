package org.hgz.ins;

/**
 * @author guozhong.huang
 */
public class LauncherTest {

    public static void main(String[] args) throws Exception {

        String[] strs = {"0", "1000", "2", "3"};


        for (int i = 0; i < 100; i++) {
            MyAtmApplication.run(strs);
        }
//        if(args[0].equals("StartMyAtmApplication")) {
//            new MyAtmApplication();
//            MyAtmApplication.run(args);
//        } else if(args[0].equals("LoadAgent")) {
//            new AgentLoader().run(args);
//        }
    }
}
