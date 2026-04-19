import pluginVue from 'eslint-plugin-vue'
import {
  configureVueProject,
  defineConfigWithVueTs,
  vueTsConfigs,
} from '@vue/eslint-config-typescript'
import prettierConfig from '@vue/eslint-config-prettier'

configureVueProject({
  tsSyntaxInTemplates: true,
})

export default defineConfigWithVueTs(
  {
    ignores: ['dist', 'node_modules', '.vite', 'coverage'],
  },
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  prettierConfig,
)
