package cn.cidea.core.utils;


import lombok.SneakyThrows;

/**
 * @author: CIdea
 */
public class HttpRequestBuilderTest {

    @SneakyThrows
    public static void get(){
        String execute = HttpRequestBuilder.get("http://erp.renhetang.cn:8200/queryGoods").execute();
        return;
    }

    public static void main(String[] args) {
        get();
    }

}