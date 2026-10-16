<script setup lang="ts">
import StatePanel from '../components/StatePanel.vue';
import CharacterIcon from '../components/CharacterIcon.vue';
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { api, query, upload } from '../api';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';
import SmartImage from '../components/SmartImage.vue';
const auth = useAuth(),
  ui = useUi(),
  router = useRouter(),
  nickname = ref(''),
  email = ref(''),
  oldPwd = ref(''),
  newPwd = ref(''),
  reNewPwd = ref(''),
  feedback = ref(''),
  code = ref(''),
  busy = ref(false),
  mail = import.meta.env.VITE_MAIL_ENABLED !== 'false';
watch(
  () => auth.user,
  (u) => {
    nickname.value = u?.nickname || '';
    email.value = u?.email || '';
  },
  { immediate: true },
);
async function action(fn: () => Promise<void>, message: string) {
  if (busy.value) return;
  busy.value = true;
  await ui.run(fn, message);
  busy.value = false;
}
async function profile() {
  await action(async () => {
    await api('/my/profile', 'PATCH', { nickname: nickname.value, email: email.value });
    await auth.refresh();
  }, '资料已更新');
}
async function password() {
  await action(async () => {
    if (newPwd.value !== reNewPwd.value) throw Error('两次新密码不一致');
    await api('/my/password', 'PATCH', {
      oldPwd: oldPwd.value,
      newPwd: newPwd.value,
      reNewPwd: reNewPwd.value,
    });
    auth.clear();
    ui.authOpen = true;
  }, '密码已修改，请重新登录');
}
async function avatar(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (file)
    await action(async () => {
      await upload('/my/avatar', file, 'PATCH');
      await auth.refresh();
    }, '头像已更新');
}
async function remove() {
  if (!(await ui.confirm('注销后账号与关联内容将被删除，无法恢复。确定继续？'))) return;
  await action(async () => {
    const id = auth.user.id;
    await api('/my/account', 'DELETE');
    Object.keys(localStorage)
      .filter((k) => k.startsWith(`vibe-draft:${id}:`) || k.startsWith(`vibe-moment:${id}:`))
      .forEach((k) => localStorage.removeItem(k));
    auth.clear();
    router.push('/');
  }, '账号已注销');
}
</script>
<template>
  <header class="section-head">
    <div>
      <span class="eyebrow">PROFILE / 我的空间</span>
      <h1>我的空间</h1>
    </div>
    <RouterLink v-if="auth.admin" to="/admin"
      ><button class="primary">✧ 管理中心</button></RouterLink
    >
  </header>
  <StatePanel v-if="!auth.user">
    <p>登录后可管理个人资料、收藏和日记。</p>
    <button @click="auth.require()">登录 / 注册</button>
  </StatePanel>
  <template v-else
    ><div class="row panel">
      <div class="row">
        <SmartImage
          v-if="auth.user.userPic"
          class="avatar-large"
          :src="auth.user.userPic"
        /><CharacterIcon v-else class="profile-character" />
        <div>
          <h2>{{ auth.user.nickname || auth.user.username }}</h2>
          <span v-if="auth.admin" class="badge">管理员 · ADMIN</span
          ><span v-else class="muted">个人账号</span>
        </div>
      </div>
      <button
        :disabled="busy"
        @click="
          action(async () => {
            await auth.logout();
            router.push('/');
          }, '已退出登录')
        "
      >
        退出登录
      </button>
    </div>
    <div class="account-grid">
      <section class="panel">
        <h2>个人资料</h2>
        <form class="form" @submit.prevent="profile">
          <label>昵称<input v-model="nickname" maxlength="100" /></label
          ><label>邮箱<input v-model="email" type="email" maxlength="254" /></label
          ><button class="primary" :disabled="busy">保存资料</button>
        </form>
        <label class="form"
          >更换头像<input
            type="file"
            accept="image/jpeg,image/png"
            :disabled="busy"
            @change="avatar"
        /></label>
      </section>
      <section class="panel">
        <h2>修改密码</h2>
        <form class="form" @submit.prevent="password">
          <label
            >当前密码<input
              v-model="oldPwd"
              type="password"
              autocomplete="current-password"
              required /></label
          ><label
            >新密码<input
              v-model="newPwd"
              type="password"
              autocomplete="new-password"
              minlength="8"
              maxlength="64"
              required /></label
          ><label
            >确认新密码<input
              v-model="reNewPwd"
              type="password"
              autocomplete="new-password"
              minlength="8"
              maxlength="64"
              required /></label
          ><button :disabled="busy">更新密码</button>
        </form>
      </section>
      <section class="panel">
        <h2>给我们捎句话</h2>
        <form
          class="form"
          @submit.prevent="
            action(async () => {
              await api('/music/feedback/addFeedback?' + query({ content: feedback }), 'POST');
              feedback = '';
            }, '谢谢你的反馈')
          "
        >
          <textarea
            v-model="feedback"
            required
            maxlength="2000"
            aria-label="反馈内容"
            placeholder="你的建议，会让这里变得更好。"
          ></textarea
          ><button :disabled="busy">发送反馈</button>
        </form>
      </section>
      <section class="panel">
        <h2>账号设置</h2>
        <p class="muted">邮箱状态：{{ auth.user.emailVerified ? '已验证' : '未验证' }}</p>
        <template v-if="mail"
          ><button
            :disabled="busy"
            @click="
              action(async () => {
                await api('/api/email/code', 'POST', { email, purpose: 'bind' });
              }, '验证码已发送')
            "
          >
            发送绑定验证码
          </button>
          <form
            class="form"
            @submit.prevent="
              action(async () => {
                await api('/my/email', 'POST', { email, code });
                await auth.refresh();
              }, '邮箱已绑定')
            "
          >
            <label>验证码<input v-model="code" required /></label
            ><button :disabled="busy">确认绑定</button>
          </form></template
        >
        <p v-else class="muted tiny">邮箱服务暂未启用，绑定验证与邮件找回功能暂不可用。</p>
        <button v-if="!auth.admin" class="danger" :disabled="busy" @click="remove">注销账号</button>
      </section>
    </div></template
  >
</template>
