import { definePlugin } from '@halo-dev/console-shared';
import { defineAsyncComponent, markRaw } from 'vue';
import { IconPalette } from "@halo-dev/components";
import { VLoading } from '@halo-dev/components';
import './styles/main.css';
import 'uno.css';

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'ToolsRoot',
      route: {
        path: 'theme-files-editor',
        name: 'ThemeFilesEditor',
        component: defineAsyncComponent({
          loader: () => import('./views/FilesEditor.vue'),
          loadingComponent: VLoading,
        }),
        meta: {
          title: '主题编辑',
          description: '可编辑主题文件',
          searchable: true,
          permissions: ['*'],
          hideFooter: true,
          menu: {
            name: '主题编辑',
            icon: markRaw(IconPalette),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
});
