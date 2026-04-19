import forms from '@tailwindcss/forms'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#5F8F89',
        accent: '#E2A86B',
        success: '#4C9076',
        warning: '#D09A57',
        danger: '#BF6765',
        info: '#5B86A3',
        surface: '#F5F1E8',
        panel: '#FFFDF9',
        text: '#1F2F2F',
        muted: '#5F6F6D',
        line: '#E4DDD0',
        ink: '#233131',
      },
      borderRadius: {
        card: '24px',
        pill: '999px',
      },
      boxShadow: {
        card: '0 18px 45px rgba(57, 73, 70, 0.08)',
        soft: '0 10px 28px rgba(88, 110, 105, 0.10)',
        float: '0 28px 60px rgba(66, 88, 84, 0.14)',
      },
      fontFamily: {
        sans: ['"Source Han Sans SC"', '"PingFang SC"', '"Microsoft YaHei"', 'Arial', 'sans-serif'],
      },
      backgroundImage: {
        'paper-glow':
          'radial-gradient(circle at top left, rgba(95,143,137,0.18), transparent 38%), radial-gradient(circle at top right, rgba(226,168,107,0.14), transparent 30%), linear-gradient(180deg, #faf7f1 0%, #f5f1e8 55%, #f0ebdf 100%)',
      },
    },
  },
  plugins: [forms],
}
