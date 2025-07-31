<script lang="ts" setup>
import { editorThemePageConsoleApiClient } from '@/api';
import { normalizePath } from '@/utils/path';
import { Toast, VButton, VModal, VSpace } from '@halo-dev/components';
import { useQueryClient } from '@tanstack/vue-query';
import { ref } from 'vue';

const props = withDefaults(defineProps<{ themeName: string; isDir?: boolean; baseDir: string }>(), {
  isDir: false,
  baseDir: '/',
});

const queryClient = useQueryClient();

const modal = ref();

const submitting = ref(false);

async function onSubmit({ path }: { path: string }) {
  try {
    submitting.value = true;

    await editorThemePageConsoleApiClient.themeFile.createFileOrDirectory({
      name: props.themeName,
      createFileRequest: {
        path: normalizePath(props.baseDir, path),
        isDir: props.isDir,
      },
    });

    Toast.success('创建成功');

    queryClient.invalidateQueries({ queryKey: ['plugin-editor-theme-page:files'] });

    modal.value.close();
  } catch (error) {
    console.error(error);
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <VModal ref="modal" :width="500" :title="isDir ? '新建文件夹' : '新建文件'">
    <FormKit id="create-file-form" type="form" name="create-file-form" @submit="onSubmit">
      <FormKit :label="isDir ? '文件夹名' : '文件名'" name="path" validation="required"></FormKit>
    </FormKit>

    <template #footer>
      <VSpace>
        <!-- @vue-ignore -->
        <VButton
          :loading="submitting"
          type="secondary"
          @click="$formkit.submit('create-file-form')"
        >
          创建
        </VButton>
        <VButton type="default" @click="modal.close()">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
