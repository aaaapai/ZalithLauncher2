#ifndef GL_LOADER_H
#define GL_LOADER_H

#include <EGL/egl.h>
#include <GL/gl.h>
#include <stdbool.h>

// GL函数指针声明
typedef void (*glFlush_func)(void);
typedef GLenum (*glGetError_func)(void);
typedef const GLubyte* (*glGetString_func)(GLenum name);

extern glFlush_func glFlush_p;
extern glGetError_func glGetError_p;
extern glGetString_func glGetString_p;

// 加载GL函数
bool dlsym_GL(void);

#endif //GL_LOADER_H
