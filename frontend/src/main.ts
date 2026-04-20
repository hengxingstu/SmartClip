import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElButton from 'element-plus/es/components/button/index'
import ElDialog from 'element-plus/es/components/dialog/index'
import ElForm, { ElFormItem } from 'element-plus/es/components/form/index'
import ElInput from 'element-plus/es/components/input/index'
import ElInputNumber from 'element-plus/es/components/input-number/index'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index'
import ElSwitch from 'element-plus/es/components/switch/index'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/form-item/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/input-number/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/option/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/switch/style/css'
import './styles.css'
import App from './App.vue'

const app = createApp(App)

app.use(createPinia())

app.component('ElButton', ElButton)
app.component('ElDialog', ElDialog)
app.component('ElForm', ElForm)
app.component('ElFormItem', ElFormItem)
app.component('ElInput', ElInput)
app.component('ElInputNumber', ElInputNumber)
app.component('ElOption', ElOption)
app.component('ElSelect', ElSelect)
app.component('ElSwitch', ElSwitch)

app.mount('#app')
