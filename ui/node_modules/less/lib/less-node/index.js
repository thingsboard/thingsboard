import environment from './environment';
import FileManager from './file-manager';
import UrlFileManager from './url-file-manager';
import createFromEnvironment from '../less';
import lesscHelper from './lessc-helper';
import PluginLoader from './plugin-loader';
import fs from './fs';
import defaultOptions from '../less/default-options';
import imageSize from './image-size';

const less = createFromEnvironment(environment, [new FileManager(), new UrlFileManager()]);

// allow people to create less with their own environment
less.createFromEnvironment = createFromEnvironment;
less.lesscHelper = lesscHelper;
less.PluginLoader = PluginLoader;
less.fs = fs;
less.FileManager = FileManager;
less.UrlFileManager = UrlFileManager;

// Set up options
less.options = defaultOptions();

// provide image-size functionality
imageSize(less.environment);

export default less;
