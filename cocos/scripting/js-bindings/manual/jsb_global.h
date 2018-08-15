#pragma once

#include <string>

namespace se {
    class Object;
    class Class;
}

extern se::Object* __ccObj;
extern se::Object* __jsbObj;
extern se::Object* __glObj;

bool jsb_register_global_variables(se::Object* global);

void jsb_init_file_operation_delegate();
bool jsb_enable_debugger(const std::string& debuggerServerAddr, uint32_t port);
bool jsb_set_extend_property(const char* ns, const char* clsName);
bool jsb_run_script(const std::string& filePath);
bool jsb_run_script_string(const char* scriptStr);
#if SCRIPT_ENGINE_TYPE == SCRIPT_ENGINE_V8
void jsb_on_surface_destroy();
void jsb_destroy_script_engine();
#endif
void jsb_set_xxtea_key(const std::string& key);
