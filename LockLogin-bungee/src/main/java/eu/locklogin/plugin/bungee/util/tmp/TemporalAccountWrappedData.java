package eu.locklogin.plugin.bungee.util.tmp;

import lombok.Builder;
import lombok.Getter;

@Builder
public class TemporalAccountWrappedData {

    @Getter(onMethod = {})
    private String name,uuid,password,pin,token,panic;
}
