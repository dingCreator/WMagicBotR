package com.whitemagic2014.pojo;

public class CustomizeCommandText {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 关键字
     */
    private String keyWord;
    /**
     * 内容
     */
    private String content;
    /**
     * 是否模糊匹配
     */
    private Boolean like;
    /**
     * 提醒人QQ号，0表示提醒发送者
     */
    private Long at;

    public CustomizeCommandText() {
    }

    public CustomizeCommandText(String keyWord, String content, Boolean like, Long at) {
        this.keyWord = keyWord;
        this.content = content;
        this.like = like;
        this.at = at;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyWord() {
        return keyWord;
    }

    public void setKeyWord(String keyWord) {
        this.keyWord = keyWord;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getLike() {
        return like;
    }

    public void setLike(Boolean like) {
        this.like = like;
    }

    public Long getAt() {
        return at;
    }

    public void setAt(Long at) {
        this.at = at;
    }
}
