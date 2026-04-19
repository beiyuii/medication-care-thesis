import type { GlobalThemeOverrides } from 'naive-ui'

/**
 * lightThemeOverrides 定义了系统的基础视觉语言，覆盖 Naive UI 默认主题。
 */
export const lightThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#5f8f89',
    primaryColorHover: '#6e9f99',
    primaryColorPressed: '#507a75',
    infoColor: '#5b86a3',
    successColor: '#4c9076',
    warningColor: '#d09a57',
    errorColor: '#bf6765',
    fontFamily: '"Source Han Sans SC","PingFang SC","Microsoft YaHei",Arial,sans-serif',
    borderRadius: '18px',
    bodyColor: '#f5f1e8',
    cardColor: '#fffdfa',
    modalColor: '#fffdfa',
    popoverColor: '#fffdfa',
    textColorBase: '#1f2f2f',
    textColor1: '#1f2f2f',
    textColor2: '#5f6f6d',
    borderColor: '#e4ddd0',
    dividerColor: '#ece5d8',
    placeholderColor: '#8b9690',
  },
  Button: {
    borderRadiusMedium: '16px',
    borderRadiusSmall: '14px',
    borderRadiusLarge: '18px',
    heightMedium: '50px',
    fontSizeMedium: '16px',
    fontWeight: '600',
    paddingMedium: '0 22px',
  },
  Card: {
    borderRadius: '24px',
    paddingMedium: '26px',
    colorEmbedded: '#fffdfa',
  },
  Tag: {
    borderRadius: '999px',
    fontWeight: '600',
  },
  Input: {
    borderRadius: '16px',
    heightMedium: '50px',
  },
  Select: {
    peers: {
      InternalSelection: {
        borderRadius: '16px',
        heightMedium: '50px',
      },
    },
  },
  Alert: {
    borderRadius: '18px',
  },
  Modal: {
    borderRadius: '28px',
  },
}
