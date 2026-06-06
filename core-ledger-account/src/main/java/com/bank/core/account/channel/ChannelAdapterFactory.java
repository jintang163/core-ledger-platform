package com.bank.core.account.channel;

import com.bank.core.common.enums.ChannelCodeEnum;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 渠道适配器工厂
 * 负责根据渠道编码获取对应的渠道适配器实现
 *
 * 设计模式：工厂模式 + 策略模式
 * 统一管理所有渠道适配器，根据渠道编码动态选择
 */
@Slf4j
@Component
public class ChannelAdapterFactory implements ApplicationContextAware {

    /** 渠道适配器缓存 */
    private final Map<String, ChannelAdapter> adapterMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 初始化时自动扫描所有ChannelAdapter实现并注册
        Map<String, ChannelAdapter> beans = applicationContext.getBeansOfType(ChannelAdapter.class);
        for (ChannelAdapter adapter : beans.values()) {
            adapterMap.put(adapter.getChannelCode(), adapter);
            log.info("注册渠道适配器: {} -> {}", adapter.getChannelCode(), adapter.getClass().getSimpleName());
        }
        log.info("渠道适配器工厂初始化完成, 共注册{}个渠道适配器", adapterMap.size());
    }

    /**
     * 根据渠道编码获取适配器
     * @param channelCode 渠道编码
     * @return 渠道适配器
     * @throws BusinessException 渠道不支持时抛出异常
     */
    public ChannelAdapter getAdapter(String channelCode) {
        ChannelAdapter adapter = adapterMap.get(channelCode);
        if (adapter == null) {
            // 如果找不到适配器，尝试使用模拟渠道
            if (ChannelCodeEnum.MOCK.getCode().equals(channelCode)) {
                return adapterMap.get(ChannelCodeEnum.MOCK.getCode());
            }
            log.error("不支持的渠道编码: {}", channelCode);
            throw new BusinessException(ResultCodeEnum.CHANNEL_NOT_SUPPORTED,
                    "不支持的渠道编码: " + channelCode);
        }
        return adapter;
    }

    /**
     * 检查渠道是否支持
     * @param channelCode 渠道编码
     * @return 是否支持
     */
    public boolean isChannelSupported(String channelCode) {
        return adapterMap.containsKey(channelCode)
                || ChannelCodeEnum.MOCK.getCode().equals(channelCode);
    }
}
