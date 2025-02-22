package cn.nbbandxdd.survey.common.wechat.msgseccheck;

import lombok.Data;

/**
 * <p>检查一段文本是否含有违法违规内容DTO。
 *
 * @author howcurious
 */
@Data
public class MsgSecCheckDTO {

    /**
     * <p>需检测的文本内容。
     */
    private String content;

    /**
     * <p>错误码。
     */
    private String errcode;

    /**
     * <p>错误信息。
     */
    private String errMsg;
}
