package org.foodApp;

import java.io.Serializable;

public enum RequestType implements Serializable {
    ADD_STORE,
    REMOVE_STORE,
    UPDATE_STORE,

    ADD_PRODUCT,
    REMOVE_PRODUCT,
    UPDATE_PRODUCT,

    SALES_REPORT,
    SALES_BY_STORE_TYPE,
    SALES_BY_PRODUCT_CATEGORY,

    DISPLAY_ALL_STORES,                // done

    FILTER_STORES, //done

    BUY_PRODUCT //done
}
