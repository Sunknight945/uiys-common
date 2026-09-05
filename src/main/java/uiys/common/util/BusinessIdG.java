package uiys.common.util;


import lombok.Getter;
import uiys.common.constant.BaseEnum;

import java.util.UUID;

public class BusinessIdG {

    @Getter
    public static enum BusinessIdType implements BaseEnum<BusinessIdType> {

        UUID(1, "UUID"),
        SNOW_ID(2, "SNOW_ID"),

        ;

        private final Integer code;
        private final String name;

        BusinessIdType(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
    }


    public static String gBid(BusinessIdType businessIdType) {
        if (businessIdType == BusinessIdType.UUID) {
            return UUID.randomUUID().toString().replaceAll("-", "");

        } else if (businessIdType == BusinessIdType.SNOW_ID) {
            return UUID.randomUUID().toString().replaceAll("-", "");
        }

        return null;
    }

}
