package com.zengame.ai.apiclient.aibase;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author jurzis.zhang
 * @version v1.0.0
 * @date 2025/6/20 10:41
 **/
@Data
public class InformationVO {

    @JsonProperty("Id")
    private Integer id;

    private String title;
    private String subtitle;
    private String description;
    private String thumb;
    private String classname;
    private String summary;
    private String tags;
    private String sourcename;
    private String author;
    private Integer status;
    private String url;
    private Integer type;
    private Integer added;
    private String addtime;
    private Integer upded;
    private String updtime;
    private Integer isshoulu;
    private String vurl;
    private Integer vsize;
    private Integer weight;
    private Integer isailog;
    private String sites;
    private String categrates;
    private Integer pv;

}
