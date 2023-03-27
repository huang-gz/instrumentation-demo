package org.hgz.ins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author guozhong.huang
 */
public class MyAtm {

    private static Logger LOGGER = LoggerFactory.getLogger(MyAtm.class);

    private static final int account = 10;

    public static void withdrawMoney(int amount) throws InterruptedException {
        //processing going on here
        Thread.sleep(2000L);
        LOGGER.info("[Application] Successful Withdrawal of [{}] units!", amount);
    }
}
