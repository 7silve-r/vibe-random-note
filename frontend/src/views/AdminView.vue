<script setup lang="ts">
import StatePanel from '../components/StatePanel.vue';
import { ref, computed, watch } from 'vue';
import { api, query, upload } from '../api';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';
import Modal from '../components/Modal.vue';
import Pager from '../components/Pager.vue';
import SmartImage from '../components/SmartImage.vue';
import AppSelect from '../components/AppSelect.vue';
import DateInput from '../components/DateInput.vue';
import { normalizeDate } from '../utils/date';
import { usePlayer } from '../stores/player';
const auth = useAuth(),
  ui = useUi(),
  tab = ref('songs'),
  rows = ref<any[]>([]),
  total = ref(0),
  page = ref(1),
  search = ref(''),
  loading = ref(false),
  error = ref(''),
  editing = ref(false),
  edit = ref<any>({}),
  busy = ref(false),
  artists = ref<any[]>([]),
  styles = ref<any[]>([]),
  selected = ref<number[]>([]),
  uploadRow = ref<any>(null),
  uploadType = ref('cover'),
  file = ref<File>(),
  binding = ref<any>(null),
  songs = ref<any[]>([]),
  songIds = ref<number[]>([]),
  styleName = ref('');
const configs: Record<string, any> = {
  songs: {
    title: '歌曲',
    key: 'songId',
    name: 'songName',
    list: 'listAdminSongs',
    add: 'addSong',
    update: 'updateSong',
    del: 'deleteSong',
    batch: 'deleteSongs',
    fields: [
      ['songName', '歌曲名称', 'text', true],
      ['artistIds', '歌手', 'artists', true],
      ['album', '专辑', 'text', true],
      ['styleIds', '风格', 'styles'],
      ['releaseTime', '发行日期', 'date', true],
    ],
  },
  artists: {
    title: '歌手',
    key: 'artistId',
    name: 'artistName',
    list: 'listArtists',
    add: 'addArtist',
    update: 'updateArtist',
    del: 'deleteArtist',
    batch: 'deleteArtists',
    fields: [
      ['artistName', '歌手名称', 'text', true],
      ['gender', '性别', 'gender', true],
      ['birth', '生日', 'date'],
      ['area', '地区', 'text'],
      ['introduction', '简介', 'textarea'],
    ],
  },
  playlists: {
    title: '歌单',
    key: 'playlistId',
    name: 'title',
    list: 'listPlaylists',
    add: 'addPlaylist',
    update: 'updatePlaylist',
    del: 'deletePlaylist',
    batch: 'deletePlaylists',
    fields: [
      ['title', '歌单名称', 'text', true],
      ['style', '风格', 'text'],
      ['introduction', '简介', 'textarea'],
    ],
  },
  banners: {
    title: '轮播图',
    key: 'bannerId',
    name: 'bannerUrl',
    list: 'listBanners',
    del: 'deleteBanner',
    batch: 'deleteBanners',
    fields: [],
  },
  feedbacks: {
    title: '反馈',
    key: 'feedbackId',
    name: 'feedback',
    list: 'listFeedback',
    del: 'deleteFeedback',
    batch: 'deleteFeedbacks',
    fields: [],
  },
  users: { title: '用户', key: 'id', name: 'username', fields: [] },
  styles: { title: '风格', key: 'styleId', name: 'name', fields: [] },
  diary: { title: '公开日记', key: 'id', name: 'title', fields: [] },
};
const config = computed(() => configs[tab.value]);
const fields = computed(() =>
  tab.value === 'users'
    ? edit.value.id
      ? [
          ['nickname', '昵称', 'text'],
          ['email', '邮箱', 'email'],
        ]
      : [
          ['username', '用户名', 'text', true],
          ['password', '初始密码', 'password', true],
          ['rePassword', '确认密码', 'password', true],
        ]
    : config.value.fields,
);
async function load() {
  if (!auth.admin) return;
  loading.value = true;
  error.value = '';
  selected.value = [];
  try {
    let d: any;
    const values = {
      pageNum: page.value,
      pageSize: 12,
      songName: search.value,
      artistName: search.value,
      title: search.value,
      keyword: search.value,
    };
    if (tab.value === 'users')
      d = await api('/admin/users?' + query({ ...values, username: search.value }));
    else if (tab.value === 'styles') d = { list: await api('/music/public/styles'), total: 0 };
    else if (tab.value === 'diary') d = await api('/public/articles?' + query(values));
    else d = await api('/music/admin/' + config.value.list, 'POST', values);
    rows.value = d.list;
    total.value = d.total || d.list.length;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
async function options() {
  const data = await api('/music/admin/listArtistNames');
  artists.value = Array.isArray(data) ? data : [];
  styles.value = await api('/music/public/styles');
}
function open(row?: any) {
  edit.value = row
    ? { ...row }
    : tab.value === 'songs'
      ? { artistIds: [], styleIds: [] }
      : tab.value === 'artists'
        ? { gender: 2 }
        : {};
  if (tab.value === 'songs' && row) {
    edit.value.artistIds = [...(row.artistIds || [])];
    edit.value.styleIds = [...(row.styleIds || [])];
  }
  editing.value = true;
}
async function save() {
  if (busy.value) return;
  busy.value = true;
  await ui.run(async () => {
    const exists = !!edit.value[config.value.key];
    const body: any = {};
    for (const [key, label, type, required] of fields.value) {
      const value = edit.value[key];
      if (required && (value == null || value === '' || (Array.isArray(value) && !value.length)))
        throw Error(`请填写${label}`);
      if (type === 'date' && value) {
        body[key] = normalizeDate(value);
        if (!body[key]) throw Error(`${label}格式不正确`);
      } else body[key] = value === '' || value == null ? undefined : value;
    }
    if (tab.value === 'songs') {
      body.artistIds = edit.value.artistIds || [];
      body.styleIds = edit.value.styleIds || [];
    }
    if (tab.value === 'artists') body.gender = Number(edit.value.gender);
    if (exists) body[config.value.key] = edit.value[config.value.key];
    if (tab.value === 'users') {
      if (!exists && body.password !== body.rePassword) throw Error('两次密码不一致');
      await api(
        exists ? `/admin/users/${edit.value.id}/profile` : '/admin/users',
        exists ? 'PATCH' : 'POST',
        body,
      );
    } else
      await api(
        '/music/admin/' + (exists ? config.value.update : config.value.add),
        exists ? 'PUT' : 'POST',
        body,
      );
    editing.value = false;
    await load();
    await options();
  }, '保存成功');
  busy.value = false;
}
async function remove(row: any) {
  if (!(await ui.confirm('确定删除这项内容？此操作无法恢复。'))) return;
  await ui.run(async () => {
    await api(
      tab.value === 'users'
        ? '/admin/users/' + row.id
        : tab.value === 'diary'
          ? '/admin/diary/articles/' + row.id
          : '/music/admin/' + config.value.del + '/' + row[config.value.key],
      'DELETE',
    );
    await load();
  }, '已删除');
}
async function batch() {
  if (!selected.value.length || !(await ui.confirm(`确定删除所选 ${selected.value.length} 项？`)))
    return;
  await ui.run(async () => {
    await api('/music/admin/' + config.value.batch, 'DELETE', selected.value);
    await load();
  }, '已删除所选内容');
}
async function status(row: any) {
  await ui.run(async () => {
    if (tab.value === 'users')
      await api(`/admin/users/${row.id}/status?status=${row.status === 0 ? 1 : 0}`, 'PATCH');
    else
      await api(`/music/admin/updateBannerStatus/${row.bannerId}?enabled=${!row.enabled}`, 'PATCH');
    await load();
  }, '状态已更新');
}
function pickUpload(row: any, type: string) {
  uploadRow.value = row;
  uploadType.value = type;
  file.value = undefined;
}
function duration(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const audio = new Audio(),
      url = URL.createObjectURL(file);
    const timer = setTimeout(() => finish(Error('无法读取音频时长')), 10000);
    function finish(error?: Error) {
      clearTimeout(timer);
      audio.removeAttribute('src');
      URL.revokeObjectURL(url);
      if (error) reject(error);
    }
    audio.onloadedmetadata = () => {
      const value = audio.duration;
      if (!Number.isFinite(value) || value <= 0) {
        finish(Error('音频时长无效'));
        return;
      }
      resolve(String(value));
      finish();
    };
    audio.onerror = () => finish(Error('无法读取音频'));
    audio.src = url;
  });
}
async function sendFile() {
  if (!file.value || busy.value) return;
  busy.value = true;
  await ui.run(async () => {
    const id = uploadRow.value[config.value.key];
    let path = `/music/admin/${tab.value}/${id}/${uploadType.value}`,
      method = 'PUT';
    if (tab.value === 'banners' && !id) {
      path = '/music/admin/banners';
      method = 'POST';
    }
    const extra: Record<string, string> =
      uploadType.value === 'audio' ? { duration: await duration(file.value!) } : {};
    await upload(path, file.value!, method, extra);
    if (uploadType.value === 'lyric' && usePlayer().current?.songId === id)
      await usePlayer().loadLyrics();
    uploadRow.value = null;
    await load();
  }, '文件已上传');
  busy.value = false;
}
async function clearLyric(row: any) {
  if (!(await ui.confirm('删除这首歌的歌词？音频仍会保留。'))) return;
  await ui.run(async () => {
    await api(`/music/admin/songs/${row.songId}/lyric`, 'DELETE');
    if (usePlayer().current?.songId === row.songId) await usePlayer().loadLyrics();
    await load();
  }, '歌词已删除');
}
async function bind(row: any) {
  await ui.run(async () => {
    const all: any[] = [];
    let n = 1;
    while (true) {
      const d = await api('/music/public/song/listSongs', 'POST', {
        pageNum: n++,
        pageSize: 100,
      });
      all.push(...d.list);
      if (all.length >= d.total || !d.list.length) break;
    }
    songs.value = all;
    const detail = await api('/music/public/playlist/getPlaylistDetail/' + row.playlistId);
    songIds.value = detail.songs.map((s: any) => s.songId);
    binding.value = row;
  });
}
watch(tab, () => {
  page.value = 1;
  search.value = '';
  load();
});
watch(
  () => auth.admin,
  () => {
    if (auth.admin) {
      load();
      ui.run(options);
    }
  },
  { immediate: true },
);
</script>
<template>
  <header class="section-head">
    <div>
      <span class="eyebrow">ADMIN / 管理</span>
      <h1>管理中心</h1>
      <p>音乐、公开日记与账号管理</p>
    </div>
    <span v-if="auth.admin" class="badge">✧ 管理员 ADMIN</span>
  </header>
  <StatePanel v-if="!auth.admin">
    此页面仅对管理员开放。<button v-if="!auth.user" @click="auth.require()">登录</button>
  </StatePanel>
  <div v-else class="admin-layout">
    <aside class="admin-menu">
      <button
        v-for="(c, key) in configs"
        :key="key"
        :class="{ active: tab === key }"
        @click="tab = String(key)"
      >
        {{ c.title }}管理
      </button>
    </aside>
    <section class="panel">
      <header class="row wrap">
        <h2>{{ config.title }}</h2>
        <button
          v-if="['songs', 'artists', 'playlists', 'users'].includes(tab)"
          class="primary"
          @click="open()"
        >
          ＋ 新建{{ config.title }}</button
        ><button v-if="tab === 'banners'" class="primary" @click="pickUpload({}, 'image')">
          ＋ 上传轮播图
        </button>
      </header>
      <form
        v-if="tab === 'styles'"
        class="toolbar"
        @submit.prevent="
          ui.run(async () => {
            await api('/music/admin/styles?' + query({ name: styleName }), 'POST');
            styleName = '';
            await load();
          }, '风格已添加')
        "
      >
        <input v-model="styleName" placeholder="新风格名称" required maxlength="50" /><button>
          添加风格
        </button>
      </form>
      <form
        v-else-if="!['banners', 'diary'].includes(tab)"
        class="toolbar"
        @submit.prevent="
          page = 1;
          load();
        "
      >
        <input
          v-model="search"
          :placeholder="'搜索' + config.title"
          aria-label="管理搜索"
        /><button>搜索</button>
      </form>
      <StatePanel v-if="loading" mood="loading" text="正在加载…" />
      <StatePanel v-else-if="error" mood="error">
        <p>{{ error }}</p>
        <button @click="load">重试</button>
      </StatePanel>
      <template v-else
        ><div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th v-if="config.batch">选择</th>
                <th>{{ config.title }}</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row[config.key]">
                <td v-if="config.batch">
                  <input
                    v-model="selected"
                    type="checkbox"
                    :value="row[config.key]"
                    :aria-label="'选择' + (row[config.name] || row[config.key])"
                  />
                </td>
                <td>
                  <template v-if="tab === 'banners'">
                    <SmartImage
                      :src="row.bannerUrl"
                      style="width: 150px; height: 65px; border-radius: 8px"
                    />
                    <small>{{ row.enabled ? '已启用' : '已停用' }}</small> </template
                  ><template v-else
                    ><RouterLink v-if="tab === 'diary'" :to="'/diary/' + row.id">{{
                      row.title
                    }}</RouterLink
                    ><span v-else>{{ row[config.name] }}</span
                    ><small v-if="row.artistName && tab === 'songs'">{{ row.artistName }}</small
                    ><small v-if="tab === 'users'">{{
                      row.role === 'ADMIN' ? '管理员' : row.status === 0 ? '正常' : '已停用'
                    }}</small></template
                  >
                </td>
                <td>
                  <template v-if="!(tab === 'users' && row.role === 'ADMIN')"
                    ><button
                      v-if="['songs', 'artists', 'playlists', 'users'].includes(tab)"
                      @click="open(row)"
                    >
                      编辑</button
                    ><button
                      v-if="['songs', 'playlists'].includes(tab)"
                      @click="pickUpload(row, 'cover')"
                    >
                      封面</button
                    ><button v-if="tab === 'songs'" @click="pickUpload(row, 'audio')">音频</button
                    ><button v-if="tab === 'songs'" @click="pickUpload(row, 'lyric')">歌词</button
                    ><button v-if="tab === 'songs' && row.lyric" @click="clearLyric(row)">
                      删除歌词</button
                    ><button v-if="tab === 'artists'" @click="pickUpload(row, 'avatar')">
                      头像</button
                    ><button v-if="tab === 'playlists'" @click="bind(row)">管理歌曲</button
                    ><button v-if="tab === 'banners'" @click="pickUpload(row, 'image')">
                      更换图片</button
                    ><button v-if="['users', 'banners'].includes(tab)" @click="status(row)">
                      {{
                        tab === 'users'
                          ? row.status === 0
                            ? '停用'
                            : '启用'
                          : row.enabled
                            ? '停用'
                            : '启用'
                      }}</button
                    ><button v-if="tab !== 'styles'" class="danger" @click="remove(row)">
                      删除
                    </button></template
                  ><span v-else class="muted">固定管理员</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <StatePanel v-if="!rows.length" :text="`暂无${config.title}，可以从新建开始。`" />
        <button v-if="config.batch && selected.length" class="danger" @click="batch">
          删除所选 {{ selected.length }} 项</button
        ><Pager
          v-if="tab !== 'styles'"
          :page="page"
          :total="total"
          @change="
            page = $event;
            load();
          "
      /></template>
    </section>
  </div>
  <Modal
    v-if="editing"
    :title="(edit[config.key] ? '编辑' : '新建') + config.title"
    @close="editing = false"
    ><form class="form" @submit.prevent="save">
      <label v-for="[key, label, type, required] in fields" :key="key"
        >{{ label
        }}<AppSelect
          v-if="type === 'artists' || type === 'styles'"
          v-model="edit[key]"
          :label="label"
          :required="!!required"
          multiple
          searchable
          :options="
            type === 'artists'
              ? artists.map((a) => ({ value: a.artistId, label: a.artistName }))
              : styles.map((s) => ({ value: s.styleId, label: s.name }))
          " />
        <AppSelect
          v-else-if="type === 'gender'"
          v-model="edit[key]"
          :label="label"
          :options="[
            { value: 0, label: '女' },
            { value: 1, label: '男' },
            { value: 2, label: '组合 / 其他' },
          ]" />
        <DateInput
          v-else-if="type === 'date'"
          v-model="edit[key]"
          :label="label"
          :required="!!required" />
        <textarea
          v-else-if="type === 'textarea'"
          v-model="edit[key]"
          :aria-label="label"
          maxlength="2000"
        ></textarea
        ><input
          v-else
          v-model="edit[key]"
          :aria-label="label"
          :type="type"
          :required="!!required"
          :minlength="type === 'password' ? 8 : undefined"
          :maxlength="type === 'password' ? 64 : 200"
      /></label>
      <p v-if="tab === 'songs' && !artists.length" class="error">请先在歌手管理中创建歌手。</p>
      <button class="primary" :disabled="busy">{{ busy ? '保存中…' : '保存' }}</button>
    </form></Modal
  ><Modal v-if="uploadRow" title="上传资源" @close="uploadRow = null"
    ><form class="form" @submit.prevent="sendFile">
      <label
        >{{
          uploadType === 'lyric'
            ? '歌词文件（UTF-8，最大 60 KB）'
            : uploadType === 'audio'
              ? '音频文件（最大 50 MB）'
              : '图片文件（最大 5 MB）'
        }}<input
          type="file"
          :accept="
            uploadType === 'lyric'
              ? '.lrc,.txt'
              : uploadType === 'audio'
                ? 'audio/*'
                : 'image/png,image/jpeg'
          "
          required
          @change="file = ($event.target as HTMLInputElement).files?.[0]" /></label
      ><button class="primary" :disabled="busy">{{ busy ? '正在上传…' : '上传' }}</button>
    </form></Modal
  ><Modal v-if="binding" :title="'管理歌曲 · ' + binding.title" @close="binding = null"
    ><form
      class="form"
      @submit.prevent="
        ui.run(async () => {
          await api('/music/admin/playlists/' + binding.playlistId + '/songs', 'PUT', songIds);
          binding = null;
        }, '歌单已更新')
      "
    >
      <div class="check-list">
        <label v-for="s in songs" :key="s.songId"
          ><input v-model="songIds" type="checkbox" :value="s.songId" />{{ s.songName }} ·
          {{ s.artistName }}</label
        >
      </div>
      <p v-if="!songs.length" class="muted">请先上传歌曲。</p>
      <button class="primary">保存歌单歌曲</button>
    </form></Modal
  >
</template>
