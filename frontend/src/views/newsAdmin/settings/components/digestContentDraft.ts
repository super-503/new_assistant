import type { NewsDigestContentSettings, NewsFeishuChannel } from '@/api/newsSetting'

export const copyDigestContent = (content?: NewsDigestContentSettings | null, showAuthor = true): NewsDigestContentSettings =>
  content
    ? {
        ...content,
        deliveryChannels: [...(content.deliveryChannels ?? ['FEISHU'])],
        channelCodes: [...content.channelCodes],
        contentTypes: [...content.contentTypes],
        fields: [...content.fields]
      }
    : {
        deliveryChannels: ['FEISHU'],
        channelCodes: [],
        contentTypes: [],
        reviewDays: null,
        fields: showAuthor ? ['author', 'url'] : ['url'],
        useAiTitle: false,
        compact: false
      }

export const savedDigestContent = (channel?: NewsFeishuChannel | null) =>
  copyDigestContent(channel?.content, channel?.card.showAuthor)
