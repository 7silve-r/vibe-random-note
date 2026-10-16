<script setup lang="ts">
import { ref } from 'vue';
import Modal from './Modal.vue';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';
import { api } from '../api';
const auth = useAuth(),
  ui = useUi();
const mail = import.meta.env.VITE_MAIL_ENABLED !== 'false';
const email = ref(''),
  code = ref('');
async function sendCode() {
  await ui.run(async () => {
    await api('/api/email/code', 'POST', { email: email.value, purpose: 'reset' });
  }, '验证码已发送');
}
const mode = ref('login'),
  username = ref(''),
  password = ref(''),
  repeat = ref(''),
  busy = ref(false),
  error = ref('');
async function submit() {
  busy.value = true;
  error.value = '';
  try {
    if (mode.value === 'reset') {
      if (password.value !== repeat.value) throw Error('两次密码不一致');
      await api('/api/password/reset', 'POST', {
        email: email.value,
        code: code.value,
        newPwd: password.value,
        reNewPwd: repeat.value,
      });
      mode.value = 'login';
      password.value = '';
      repeat.value = '';
      ui.notify('密码已重置，请登录');
      return;
    }
    if (mode.value === 'register') {
      if (password.value !== repeat.value) throw Error('两次密码不一致');
      await api('/api/reg', 'POST', {
        username: username.value,
        password: password.value,
        rePassword: repeat.value,
      });
    }
    await auth.login(username.value, password.value);
    ui.authOpen = false;
    ui.notify(auth.admin ? '欢迎回来，管理员' : '欢迎回来');
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <Modal title="很高兴遇见你" @close="ui.authOpen = false"
    ><p class="muted">使用同一账号访问音乐与日记。</p>
    <div class="tabs">
      <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button
      ><button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
    </div>
    <button v-if="mail" class="tiny" @click="mode = mode === 'reset' ? 'login' : 'reset'">
      {{ mode === 'reset' ? '返回登录' : '忘记密码' }}
    </button>
    <form class="form" @submit.prevent="submit">
      <template v-if="mode === 'reset'"
        ><label>邮箱<input v-model="email" type="email" required /></label
        ><button type="button" @click="sendCode">发送验证码</button
        ><label>验证码<input v-model="code" required /></label
      ></template>
      <label v-if="mode !== 'reset'"
        >用户名<input
          v-model="username"
          autocomplete="username"
          required
          :pattern="mode === 'register' ? '[a-zA-Z0-9_]{5,30}' : undefined"
          placeholder="5—30 位字母、数字或下划线" /></label
      ><label
        >密码<input
          v-model="password"
          type="password"
          :autocomplete="mode === 'register' ? 'new-password' : 'current-password'"
          required
          minlength="8"
          maxlength="64" /></label
      ><label v-if="mode !== 'login'"
        >确认密码<input v-model="repeat" type="password" autocomplete="new-password" required
      /></label>
      <p v-if="error" role="alert" class="error">{{ error }}</p>
      <button class="primary" :disabled="busy">
        {{
          busy
            ? '请稍候…'
            : mode === 'login'
              ? '登录，继续旅程'
              : mode === 'reset'
                ? '重置密码'
                : '创建账号'
        }}
      </button>
    </form></Modal
  >
</template>
