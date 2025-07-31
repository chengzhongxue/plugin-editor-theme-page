<script lang="ts" setup>
import { VButton, VModal} from '@halo-dev/components';
import { ref } from 'vue';

withDefaults(defineProps<{ themeName: string; path?: string }>(), {
  path: '/',
});

const emit = defineEmits<{
  (event: 'close'): void;
}>();

const modal = ref();

const autoUnzip = ref(false);
</script>

<template>
  <VModal
    ref="modal"
    title="上传文件"
    :width="920"
    height="calc(100vh - 20px)"
    :centered="false"
    @close="emit('close')"
  >
    <FormKit
      v-model="autoUnzip"
      type="checkbox"
      label="自动解压"
      help="建议仅上传 zip 时开启"
    ></FormKit>
    <UppyUpload
      :endpoint="`/apis/console.api.editorthemepage.kunkunyu.com/v1alpha1/themefiles/${themeName}/upload`"
      :meta="{
        dir: path,
        unzip: autoUnzip ? 'true' : 'false',
      }"
      width="100%"
      :allowed-meta-fields="['unzip', 'dir']"
      :done-button-handler="() => emit('close')"
    />
    <template #footer>
      <VButton @click="modal.close()">关闭</VButton>
    </template>
  </VModal>
</template>
