let fs;
try
{
    fs = require('graceful-fs');
}
catch (e)
{
    fs = require('fs');
}
export default fs;
