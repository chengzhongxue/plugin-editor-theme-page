import { axiosInstance } from '@halo-dev/api-client';
import { ConsoleApiEditorthemepageKunkunyuComV1alpha1ThemeFileApi } from './generated';


const editorThemePageConsoleApiClient = {
  themeFile: new ConsoleApiEditorthemepageKunkunyuComV1alpha1ThemeFileApi(undefined, '', axiosInstance),
};

export { editorThemePageConsoleApiClient };
