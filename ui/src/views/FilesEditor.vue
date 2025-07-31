<script lang="ts" setup>
import { consoleApiClient } from "@halo-dev/api-client";
import { VAvatar, VButton, VCard, VPageHeader, VSpace, Dialog, Toast } from '@halo-dev/components';
import {useQuery, useQueryClient} from '@tanstack/vue-query';
import { useLocalStorage } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { useRoute } from 'vue-router';
import FileContentEditor from './components/FileContentEditor.vue';
import FilesTreeSection from './components/FilesTreeSection.vue';
import {ref} from "vue";
import FileUploadModal from "@/components/FileUploadModal.vue";

const route = useRoute();
const queryClient = useQueryClient();

const showSidebar = useLocalStorage('plugin-editor-theme-page:show-sidebar', true);

const { data: theme } = useQuery({
  queryKey: ['plugin-editor-theme-page:theme'],
  queryFn: async () => {
    const { data } = await consoleApiClient.theme.theme.fetchActivatedTheme();
    return data;
  },
});

async function handleClearCache() {
  Dialog.warning({
    title: "清除模板缓存",
    description: "此功能适用于在运行时修改模板文件后，刷新缓存以查看最新网页结果。",
    confirmText: "确认",
    cancelText: "取消",
    async onConfirm() {
      if (!theme.value) {
        console.error("No selected or activated theme");
        return;
      }

      await consoleApiClient.theme.theme.invalidateCache({
        name: theme.value?.metadata.name,
      });

      Toast.success("操作成功");
    },
  });
}

const handleReloadTheme = async () => {
  Dialog.warning({
    title: "重载主题配置",
    description: "该操作仅会重载主题配置和设置表单定义，不会删除已保存的配置。",
    confirmText: "确认",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        if (!theme?.value) {
          return;
        }

        await consoleApiClient.theme.theme.reload({
          name: theme.value.metadata.name as string,
        });

        Toast.success("重载主题配置成功");

        window.location.reload();
      } catch (e) {
        console.error("Failed to reload theme setting", e);
      }
    },
  });
};

// Upload
const uploadModalVisible = ref(false);
const selectedDir = useRouteQuery<string>('dir', '/', { mode: 'push' });

function openUploadModal(path: string) {
  uploadModalVisible.value = true;
  selectedDir.value = path
}

function onUploadModalClose() {
  uploadModalVisible.value = false;
  queryClient.invalidateQueries([
    'plugin-editor-theme-page:files',
    theme.value?.metadata.name,
  ]);
}

const selectedFilePath = useRouteQuery<string | undefined>('path');


</script>

<template>
  <FileUploadModal
    v-if="uploadModalVisible && theme?.metadata.name"
    :theme-name="theme?.metadata.name"
    :path="selectedDir"
    @close="onUploadModalClose"
  />
  <VPageHeader :title="theme?.spec.displayName || '加载中...'">
    <template #icon>
      <VAvatar
        :src="theme?.spec.logo"
        class=":uno: mr-2 self-center"
        :alt="theme?.spec.displayName"
        size="xs"
      />
    </template>
    <template #actions>
      <VSpace>
        <VButton size="sm" @click="$router.back()"> 返回</VButton>
        <VButton type="secondary" @click="handleReloadTheme">重载主题配置</VButton>
        <VButton type="secondary" @click="handleClearCache">清理模版缓存</VButton>
      </VSpace>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 rounded bg-white md:m-4">
    <VCard style="height: calc(100vh - 5.5rem)" :body-class="['editor-theme-page-card-body']">
      <div
        class=":uno: grid h-full grid-cols-12 divide-y sm:divide-x sm:divide-y-0"
        :class="{ ':uno: !divide-none': !showSidebar }"
      >
        <div
          v-show="showSidebar"
          class=":uno: relative col-span-12 h-full overflow-auto p-2 sm:col-span-6 lg:col-span-5 xl:col-span-3"
        >
          <FilesTreeSection v-if="theme" v-model="selectedFilePath"  @open:upload:modal="openUploadModal" :theme-name="theme.metadata.name" />
        </div>

        <div
          class=":uno: col-span-12 sm:col-span-6 lg:col-span-7 xl:col-span-9"
          :class="{ ':uno: !col-span-12': !showSidebar }"
        >
          <FileContentEditor v-if="theme" :theme-name="theme.metadata.name" :path="selectedFilePath" />
        </div>
      </div>
    </VCard>
  </div>
</template>
