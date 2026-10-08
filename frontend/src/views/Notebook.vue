<template>
  <div
    class="notebook-shell flex h-full min-w-[980px] overflow-hidden bg-white text-zinc-800"
    :class="isSidePanelResizing || isRightPanelResizing ? 'select-none' : ''"
  >
    <aside
      v-show="sidePanelVisible"
      class="relative flex w-[318px] shrink-0 flex-col border-r border-zinc-200 bg-[#fbfbfa]"
      :style="{ width: `${sidePanelWidth}px` }"
    >
      <header class="relative flex h-14 shrink-0 items-center justify-center border-b border-zinc-200 px-3">
        <button class="icon-button absolute left-3" title="收起笔记侧栏" @click="sidePanelVisible = false">
          <PanelLeftClose class="h-[18px] w-[18px]" />
        </button>
        <nav class="flex items-center gap-1" aria-label="笔记视图">
          <button
            v-for="tab in sideTabs"
            :key="tab.key"
            class="side-tab"
            :class="sideTab === tab.key ? 'side-tab-active' : ''"
            :title="tab.label"
            :aria-label="tab.label"
            @click="sideTab = tab.key"
          >
            <component :is="tab.icon" class="h-[18px] w-[18px]" />
          </button>
        </nav>
      </header>

      <div v-if="sideTab === 'folders'" class="file-action-bar">
        <button class="icon-button" title="新建笔记" @click="createPrototypeNote">
          <SquarePen class="h-[19px] w-[19px]" />
        </button>
        <button class="icon-button" title="新建文件夹">
          <FolderPlus class="h-[19px] w-[19px]" />
        </button>
        <div ref="sortMenuRef" class="relative">
          <button class="icon-button" :class="showSortMenu ? 'file-action-active' : ''" title="排序" @click="showSortMenu = !showSortMenu">
            <ArrowUpDown class="h-[19px] w-[19px]" />
          </button>
          <div v-if="showSortMenu" class="sort-menu">
            <template v-for="option in sortOptions" :key="option.value">
              <button class="sort-menu-item" :class="sortOption === option.value ? 'sort-menu-item-active' : ''" @click="selectSortOption(option.value)">
                <span>{{ option.label }}</span>
                <Check v-if="sortOption === option.value" class="h-4 w-4" />
              </button>
              <div v-if="option.dividerAfter" class="sort-menu-divider" />
            </template>
          </div>
        </div>
        <button class="icon-button" title="自动显示当前文件" @click="revealActiveNote">
          <PanelTop class="h-[19px] w-[19px]" />
        </button>
        <button class="icon-button" :title="allFoldersCollapsed ? '展开全部文件夹' : '折叠全部文件夹'" @click="toggleAllFolders">
          <span class="flex flex-col items-center -space-y-1.5" aria-hidden="true">
            <ChevronUp v-if="allFoldersCollapsed" class="h-3.5 w-3.5" />
            <ChevronDown v-else class="h-3.5 w-3.5" />
            <ChevronDown v-if="allFoldersCollapsed" class="h-3.5 w-3.5" />
            <ChevronUp v-else class="h-3.5 w-3.5" />
          </span>
        </button>
      </div>

      <section class="min-h-0 flex-1 overflow-y-auto px-3 py-4">
        <template v-if="sideTab === 'folders'">
          <div class="mb-3 flex items-center justify-between px-2">
            <span class="text-xs font-medium tracking-wide text-zinc-400">目录</span>
            <span class="text-xs text-zinc-400">{{ notes.length }} 篇</span>
          </div>

          <div v-for="folder in folders" :key="folder.id" class="mb-4">
            <button class="folder-heading" @click="toggleFolder(folder.id)">
              <ChevronDown class="h-4 w-4 transition-transform" :class="collapsedFolders.includes(folder.id) ? '-rotate-90' : ''" />
              <Folder class="h-[17px] w-[17px] text-[#58a889]" />
              <span class="truncate">{{ folder.name }}</span>
              <span class="ml-auto text-xs font-normal text-zinc-400">{{ notesInFolder(folder.id).length }}</span>
            </button>
            <div v-show="!collapsedFolders.includes(folder.id)" class="mt-1 space-y-0.5">
              <button
                v-for="note in notesInFolder(folder.id)"
                :key="note.id"
                class="tree-note-row"
                :class="activeNoteId === note.id ? 'tree-note-row-active' : ''"
                :data-note-id="note.id"
                @click="selectNote(note.id)"
              >
                <FileText class="h-[16px] w-[16px] shrink-0 text-zinc-400" />
                <span class="truncate">{{ note.title }}</span>
              </button>
            </div>
          </div>

          <div class="mb-2 mt-6 px-2 text-xs font-medium tracking-wide text-zinc-400">未分类</div>
          <div class="space-y-0.5">
            <button
              v-for="note in notesInFolder(null)"
              :key="note.id"
              class="tree-note-row"
              :class="activeNoteId === note.id ? 'tree-note-row-active' : ''"
              :data-note-id="note.id"
              @click="selectNote(note.id)"
            >
              <FileText class="h-[16px] w-[16px] shrink-0 text-zinc-400" />
              <span class="truncate">{{ note.title }}</span>
            </button>
          </div>
        </template>

        <template v-else-if="sideTab === 'recent'">
          <div class="recent-view-toolbar">
            <div ref="recentFilterMenuRef" class="relative inline-block">
              <button class="recent-filter" :class="showRecentFilterMenu ? 'recent-filter-active' : ''" title="选择文件夹" @click="showRecentFilterMenu = !showRecentFilterMenu">
                {{ recentFilterLabel }}
                <ChevronDown class="h-4 w-4" />
              </button>
              <div v-if="showRecentFilterMenu" class="recent-filter-menu">
                <button class="recent-filter-menu-item" :class="recentFolderFilter === 'all' ? 'recent-filter-menu-item-active' : ''" @click="selectRecentFolder('all')">
                  <NotepadText class="h-5 w-5" />
                  <span>全部笔记</span>
                </button>
                <div class="recent-filter-menu-divider" />
                <button
                  v-for="folder in folders"
                  :key="folder.id"
                  class="recent-filter-menu-item recent-folder-menu-item"
                  :class="recentFolderFilter === folder.id ? 'recent-filter-menu-item-active' : ''"
                  @click="selectRecentFolder(folder.id)"
                >
                  <Folder class="h-5 w-5" />
                  <span class="min-w-0 flex-1">
                    <span class="block truncate">{{ folder.name }}</span>
                    <span class="mt-0.5 block text-xs font-normal text-zinc-400">{{ notesInFolder(folder.id).length }} 篇笔记</span>
                  </span>
                </button>
              </div>
            </div>
            <div ref="recentSortMenuRef" class="relative">
              <button class="icon-button" :class="showRecentSortMenu ? 'file-action-active' : ''" title="排序" @click="showRecentSortMenu = !showRecentSortMenu">
                <ArrowUpDown class="h-[18px] w-[18px]" />
              </button>
              <div v-if="showRecentSortMenu" class="sort-menu recent-sort-menu">
                <template v-for="option in sortOptions" :key="option.value">
                  <button class="sort-menu-item" :class="recentSortOption === option.value ? 'sort-menu-item-active' : ''" @click="selectRecentSortOption(option.value)">
                    <span>{{ option.label }}</span>
                    <Check v-if="recentSortOption === option.value" class="h-4 w-4" />
                  </button>
                  <div v-if="option.dividerAfter" class="sort-menu-divider" />
                </template>
              </div>
            </div>
          </div>
          <section v-for="group in recentGroups" :key="group.label" class="mb-7">
            <h2 class="mb-2 px-1 text-xs font-medium text-zinc-400">{{ group.label }}</h2>
            <div class="space-y-1">
              <button
                v-for="note in group.notes"
                :key="note.id"
                class="recent-note-row"
                :class="activeNoteId === note.id ? 'recent-note-row-active' : ''"
                @click="selectNote(note.id)"
              >
                <span class="flex items-start justify-between gap-3">
                  <span class="truncate text-sm font-semibold text-zinc-800">{{ note.title }}</span>
                  <span class="shrink-0 text-xs text-zinc-400">{{ note.updatedLabel }}</span>
                </span>
                <span class="mt-1.5 line-clamp-2 text-left text-xs leading-5 text-zinc-500">{{ excerptFor(note) }}</span>
                <button
                  v-if="recentFolderFilter === 'all' && note.folderId"
                  class="recent-folder-link"
                  :title="`筛选 ${folderName(note.folderId)} 的笔记`"
                  @click.stop="selectRecentFolder(note.folderId)"
                >
                  <Folder class="h-3.5 w-3.5" />
                  {{ folderName(note.folderId) }}
                </button>
              </button>
            </div>
          </section>
        </template>

        <template v-else-if="sideTab === 'search'">
          <label class="relative block">
            <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
            <input v-model="searchKeyword" class="search-input" placeholder="搜索标题和正文" autofocus />
            <button v-if="searchKeyword" class="absolute right-2 top-1/2 inline-flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-full text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700" title="清空搜索" @click.prevent="searchKeyword = ''">
              <X class="h-4 w-4" />
            </button>
          </label>
          <template v-if="searchKeyword.trim()">
            <div class="mb-3 mt-4 flex items-center justify-between px-1">
              <span class="text-xs text-zinc-400">{{ searchResults.length }} 项结果</span>
              <div ref="searchSortMenuRef" class="relative">
                <button class="search-sort-button" @click="showSearchSortMenu = !showSearchSortMenu">
                  {{ searchSortLabel }}
                  <ChevronDown class="h-3.5 w-3.5" />
                </button>
                <div v-if="showSearchSortMenu" class="search-sort-menu">
                  <template v-for="option in sortOptions" :key="option.value">
                    <button class="search-sort-menu-item" :class="searchSort === option.value ? 'search-sort-menu-item-active' : ''" @click="selectSearchSort(option.value)">
                      {{ option.label }} <Check v-if="searchSort === option.value" class="h-4 w-4" />
                    </button>
                    <div v-if="option.dividerAfter" class="search-sort-menu-divider" />
                  </template>
                </div>
              </div>
            </div>
            <div v-if="searchResults.length" class="space-y-1">
              <button v-for="note in searchResults" :key="note.id" class="search-result-row" @click="openSearchResult(note.id)">
                <span class="flex items-start justify-between gap-3">
                  <span class="line-clamp-1 text-left text-sm font-semibold text-zinc-800" v-html="highlightSearchText(note.title)" />
                  <span class="shrink-0 text-xs text-zinc-400">{{ note.updatedLabel }}</span>
                </span>
                <span class="mt-1.5 line-clamp-2 text-left text-xs leading-5 text-zinc-500" v-html="highlightSearchText(searchExcerptFor(note))" />
                <span class="mt-2 flex items-center justify-between text-xs text-zinc-400">
                  <span class="flex items-center gap-1.5"><Folder class="h-3.5 w-3.5" />{{ folderName(note.folderId) }}</span>
                  <span>{{ searchMatchCount(note) }} 处匹配</span>
                </span>
              </button>
            </div>
            <div v-else class="search-empty-state">没有找到“{{ searchKeyword }}”相关的笔记。</div>
          </template>
          <div v-else class="search-start-state">
            <Search class="h-5 w-5" />
            输入关键词，搜索笔记标题和正文
          </div>
        </template>

      </section>

      <footer v-if="sideTab !== 'search'" class="flex h-11 shrink-0 items-center border-t border-zinc-200 px-4 text-xs text-zinc-400">
        {{ notes.length }} 篇笔记 · {{ folders.length }} 个文件夹
      </footer>
      <div class="side-panel-resizer" title="拖拽调整宽度" @mousedown.prevent="startSidePanelResize" />
    </aside>

    <main class="flex min-w-0 flex-1 flex-col bg-white">
      <div class="editor-toolbar">
        <button class="format-button" title="撤销" @click="runRichCommand('undo')"><Undo2 class="h-4 w-4" /></button>
        <button class="format-button" title="重做" @click="runRichCommand('redo')"><Redo2 class="h-4 w-4" /></button>
        <button class="format-button" title="格式刷"><Paintbrush class="h-4 w-4" /></button>
        <button class="format-button" title="清除格式" @click="runRichCommand('removeFormat')"><Eraser class="h-4 w-4" /></button>
        <span class="toolbar-divider" />
        <div ref="insertMenuRef" class="relative">
          <button class="format-select" :class="showInsertMenu ? 'format-select-active' : ''" title="插入" @click="showInsertMenu = !showInsertMenu"><CirclePlus class="h-4 w-4" />插入 <ChevronDown class="h-3.5 w-3.5" /></button>
          <div v-if="showInsertMenu" class="insert-menu">
            <button class="insert-menu-item"><Table2 class="h-4 w-4" />表格</button>
            <button class="insert-menu-item" @click="insertRichLink"><Link class="h-4 w-4" />链接</button>
            <button class="insert-menu-item"><ImagePlus class="h-4 w-4" />图片</button>
            <div class="toolbar-menu-divider" />
            <button class="insert-menu-item" @click="formatRichBlock('pre')"><Code2 class="h-4 w-4" />代码块</button>
            <button class="insert-menu-item"><Sigma class="h-4 w-4" />公式</button>
            <button class="insert-menu-item" @click="insertHorizontalRule"><Minus class="h-4 w-4" />分割线</button>
            <button class="insert-menu-item" @click="formatRichBlock('blockquote')"><Quote class="h-4 w-4" />引用</button>
            <div class="toolbar-menu-divider" />
            <button class="insert-menu-item"><Mic class="h-4 w-4" />录音纪要</button>
            <button class="insert-menu-item"><Paperclip class="h-4 w-4" />附件</button>
            <button class="insert-menu-item" @click="insertWikiLink"><NotebookPen class="h-4 w-4" />笔记</button>
          </div>
        </div>
        <span class="toolbar-divider" />
        <button class="format-button font-bold" title="加粗" @click="runRichCommand('bold')">B</button>
        <button class="format-button font-serif text-base italic" title="斜体" @click="runRichCommand('italic')">I</button>
        <button class="format-button underline" title="下划线" @click="runRichCommand('underline')">U</button>
        <button class="format-button" title="删除线" @click="runRichCommand('strikeThrough')"><Strikethrough class="h-4 w-4" /></button>
        <div ref="highlightMenuRef" class="relative">
          <button class="format-button" :class="showHighlightMenu ? 'format-button-active' : ''" title="高亮" @click="showHighlightMenu = !showHighlightMenu"><Highlighter class="h-4 w-4" /></button>
          <div v-if="showHighlightMenu" class="color-menu"><p>背景颜色</p><button class="color-reset" @click="applyRichColor('hiliteColor', 'transparent')">无颜色</button><div class="color-grid"><button v-for="color in highlightColors" :key="color" :style="{ background: color }" @click="applyRichColor('hiliteColor', color)" /></div></div>
        </div>
        <div ref="textColorMenuRef" class="relative">
          <button class="format-button text-color-button" :class="showTextColorMenu ? 'format-button-active' : ''" title="文字颜色" @click="showTextColorMenu = !showTextColorMenu">A</button>
          <div v-if="showTextColorMenu" class="color-menu"><p>文字颜色</p><button class="color-reset" @click="applyRichColor('foreColor', '#3f3f46')">默认颜色</button><div class="color-grid"><button v-for="color in textColors" :key="color" :style="{ background: color }" @click="applyRichColor('foreColor', color)" /></div></div>
        </div>
        <div ref="blockMenuRef" class="relative">
          <button class="format-select" :class="showBlockMenu ? 'format-select-active' : ''" title="切换格式" @click="showBlockMenu = !showBlockMenu">正文 1 <ChevronDown class="h-3.5 w-3.5" /></button>
          <div v-if="showBlockMenu" class="block-menu"><button v-for="block in blockOptions" :key="block.tag" :class="`block-menu-${block.tag}`" @click="selectBlockFormat(block.tag)">{{ block.label }}</button></div>
        </div>
        <span class="toolbar-divider" />
        <div ref="alignMenuRef" class="relative">
          <button class="format-button" :class="showAlignMenu ? 'format-button-active' : ''" title="对齐方式" @click="showAlignMenu = !showAlignMenu"><AlignLeft class="h-4 w-4" /></button>
          <div v-if="showAlignMenu" class="align-menu"><button v-for="align in alignOptions" :key="align.value" :title="align.label" @click="setRichAlignment(align.value)"><component :is="align.icon" class="h-4 w-4" /></button></div>
        </div>
        <button class="format-button" title="待办列表" @click="insertTodo"><ListTodo class="h-4 w-4" /></button>
        <button class="format-button" title="无序列表" @click="runRichCommand('insertUnorderedList')"><List class="h-4 w-4" /></button>
        <button class="format-button" title="有序列表" @click="runRichCommand('insertOrderedList')"><ListOrdered class="h-4 w-4" /></button>
        <button class="format-button" title="减少缩进" @click="runRichCommand('outdent')"><IndentDecrease class="h-4 w-4" /></button>
        <button class="format-button" title="增加缩进" @click="runRichCommand('indent')"><IndentIncrease class="h-4 w-4" /></button>
        <button class="format-button" title="查找和替换" @click="showFindPanel = !showFindPanel"><FileSearch class="h-4 w-4" /></button>

        <div class="ml-auto flex items-center gap-1">
          <button class="mode-button" :class="viewMode === 'writing' ? 'mode-button-active' : ''" :title="viewMode === 'writing' ? '切换到阅读模式' : '切换到编辑模式'" @click="setViewMode(viewMode === 'writing' ? 'reading' : 'writing')"><PenLine v-if="viewMode === 'writing'" class="h-4 w-4" /><Eye v-else class="h-4 w-4" /></button>
          <div ref="moreMenuRef" class="relative ml-1">
            <button class="icon-button" title="更多操作" @click="showMoreMenu = !showMoreMenu"><MoreHorizontal class="h-[19px] w-[19px]" /></button>
            <div v-if="showMoreMenu" class="more-menu">
              <button class="more-menu-item"><Library class="h-4 w-4" />加入知识库</button>
              <button class="more-menu-item"><Download class="h-4 w-4" />导出 Markdown</button>
              <button class="more-menu-item"><Trash2 class="h-4 w-4" />移至回收站</button>
            </div>
          </div>
          <button v-if="!rightPanelVisible" class="icon-button ml-1" title="展开上下文面板" @click="rightPanelVisible = true"><PanelRightOpen class="h-[18px] w-[18px]" /></button>
        </div>
      </div>

      <div v-if="showFindPanel" class="find-panel">
        <Search class="h-4 w-4 text-zinc-400" /><input placeholder="查找当前笔记" /><button class="find-panel-arrow" title="上一个匹配"><ChevronUp class="h-4 w-4" /></button><button class="find-panel-arrow" title="下一个匹配"><ChevronDown class="h-4 w-4" /></button><button class="icon-button" title="关闭查找" @click="showFindPanel = false"><X class="h-4 w-4" /></button>
      </div>

      <article class="min-h-0 flex-1 overflow-y-auto bg-white">
        <div class="editor-page">
          <input v-model="activeNote.title" class="title-input" placeholder="无标题笔记" @input="touchActiveNote" />
          <div
            v-if="viewMode === 'writing'"
            ref="richEditorRef"
            class="rich-editor"
            contenteditable="true"
            spellcheck="false"
            data-placeholder="开始记录你的想法..."
            @input="syncRichEditor"
          />
          <textarea v-else-if="viewMode === 'source'" ref="sourceEditorRef" v-model="noteContent" class="source-editor" spellcheck="false" @input="touchActiveNote" />
          <section v-else class="reading-content" v-html="renderedNoteContent" />
        </div>
      </article>
    </main>

    <aside
      v-show="rightPanelVisible"
      class="relative flex w-[320px] shrink-0 flex-col border-l border-zinc-200 bg-[#fbfbfa]"
      :style="{ width: `${rightPanelWidth}px` }"
    >
      <div class="side-panel-resizer right-resizer" title="拖拽调整宽度" @mousedown.prevent="startRightPanelResize" />
      <header class="relative flex h-14 shrink-0 items-center gap-1 border-b border-zinc-200 px-3">
        <button v-for="tab in rightTabs" :key="tab.key" class="side-tab" :class="rightTab === tab.key ? 'side-tab-active' : ''" :title="tab.label" @click="rightTab = tab.key">
          <component :is="tab.icon" class="h-[18px] w-[18px]" />
        </button>
        <button class="icon-button absolute right-3" title="收起上下文面板" @click="rightPanelVisible = false"><PanelRightClose class="h-[18px] w-[18px]" /></button>
      </header>

      <section v-if="rightTab === 'backlinks'" class="min-h-0 flex-1 overflow-y-auto p-4">
        <p class="mb-3 text-xs font-medium tracking-wide text-zinc-400">链接到 {{ activeNote.title }} 的笔记</p>
        <div v-if="backlinkNotes.length" class="space-y-1">
          <button v-for="note in backlinkNotes" :key="note.id" class="link-note-row" @click="selectNote(note.id)"><FileText class="h-4 w-4" /><span class="truncate">{{ note.title }}</span><ChevronRight class="ml-auto h-4 w-4 text-zinc-300" /></button>
        </div>
        <div v-else class="empty-state">还没有笔记链接到当前笔记。</div>
      </section>

      <section v-else-if="rightTab === 'outgoing'" class="min-h-0 flex-1 overflow-y-auto p-4">
        <p class="mb-3 text-xs font-medium tracking-wide text-zinc-400">当前笔记关联的笔记</p>
        <div v-if="outgoingNotes.length" class="space-y-1">
          <button v-for="note in outgoingNotes" :key="note.id" class="link-note-row" @click="selectNote(note.id)"><FileText class="h-4 w-4" /><span class="truncate">{{ note.title }}</span><ChevronRight class="ml-auto h-4 w-4 text-zinc-300" /></button>
        </div>
        <div v-else class="empty-state">在正文中插入关联笔记后，会显示在这里。</div>
      </section>

      <section v-else class="flex min-h-0 flex-1 flex-col">
        <div class="ai-conversation-wrap">
          <div ref="aiConversationRef" class="ai-conversation-scroll">
            <div class="min-w-[350px] space-y-4 px-4 py-5">
              <div class="ai-context-chip"><FileText class="h-3.5 w-3.5" />当前笔记：{{ activeNote.title }}</div>
              <div v-for="message in activeConversationMessages" :key="message.id" class="ai-message" :class="message.role === 'user' ? 'ai-message-user' : 'ai-message-assistant'">
                <span class="ai-message-role">{{ message.role === 'user' ? '你' : 'AI' }}</span>
                <p>{{ message.content }}</p>
              </div>
            </div>
          </div>
          <div class="ai-scroll-controls">
            <button title="滚到顶部" @click="scrollAiConversation('top')"><ChevronsUp class="h-4 w-4" /></button>
            <button title="上一条提问" @click="scrollAiConversation('previous')"><ChevronUp class="h-4 w-4" /></button>
            <button title="下一条提问" @click="scrollAiConversation('next')"><ChevronDown class="h-4 w-4" /></button>
            <button title="滚到底部" @click="scrollAiConversation('bottom')"><ChevronsDown class="h-4 w-4" /></button>
          </div>
        </div>

        <div class="ai-session-bar">
          <div class="flex items-center gap-1">
            <button v-for="session in conversationSessions" :key="session.id" class="ai-session-tab" :class="activeSessionId === session.id ? 'ai-session-tab-active' : ''" @click="activeSessionId = session.id">{{ session.label }}</button>
          </div>
          <div class="ml-auto flex items-center gap-1">
            <button class="icon-button" title="新建会话框" @click="addConversationSession"><SquarePlus class="h-4 w-4" /></button>
            <button class="icon-button" title="新对话" @click="startNewConversation"><MessageSquarePlus class="h-4 w-4" /></button>
            <div ref="conversationHistoryRef" class="relative">
              <button class="icon-button" title="对话历史" @click="showConversationHistory = !showConversationHistory"><History class="h-4 w-4" /></button>
              <div v-if="showConversationHistory" class="conversation-history-menu">
                <p>对话历史</p>
                <button v-for="item in conversationHistory" :key="item.id" @click="restoreConversation(item)"><MessageSquare class="h-4 w-4" /><span><strong>{{ item.title }}</strong><small>{{ item.time }}</small></span></button>
              </div>
            </div>
          </div>
        </div>

        <footer class="ai-compose">
          <textarea v-model="aiInput" rows="3" class="ai-input" placeholder="基于当前笔记提问..." @keydown.enter.exact.prevent="sendAiMessage" />
          <div class="ai-compose-tools">
            <div ref="modelMenuRef" class="relative">
              <button class="ai-control-text" @click="showModelMenu = !showModelMenu"><Bot class="h-4 w-4" />{{ selectedModel }}<ChevronDown class="h-3.5 w-3.5" /></button>
              <div v-if="showModelMenu" class="ai-popover model-menu"><button v-for="model in models" :key="model" @click="selectModel(model)">{{ model }}</button></div>
            </div>
            <div ref="thinkingMenuRef" class="relative">
              <button class="ai-control-text" @click="showThinkingMenu = !showThinkingMenu">Thinking: <strong>{{ thinkingMode }}</strong></button>
              <div v-if="showThinkingMenu" class="ai-popover thinking-menu"><button v-for="mode in thinkingModes" :key="mode" @click="selectThinkingMode(mode)">{{ mode }}</button></div>
            </div>
            <span class="ai-context-usage">16%</span>
            <button class="icon-button" title="上传文件"><Upload class="h-4 w-4" /></button>
            <button class="send-button" :class="aiInput.trim() ? 'send-button-active' : ''" :disabled="!aiInput.trim()" title="发送" @click="sendAiMessage"><CircleArrowUp class="h-4 w-4" /></button>
          </div>
        </footer>
      </section>
    </aside>

    <footer class="note-status-bar">
      <span>{{ backlinkCount }} 条反向链接</span>
      <div ref="statusModeMenuRef" class="relative">
        <button class="status-mode-button" title="切换视图模式" @click="showStatusModeMenu = !showStatusModeMenu"><PenLine v-if="viewMode === 'writing'" class="h-4 w-4" /><Code2 v-else-if="viewMode === 'source'" class="h-4 w-4" /><Eye v-else class="h-4 w-4" /></button>
        <div v-if="showStatusModeMenu" class="status-mode-menu">
          <button :class="viewMode === 'reading' ? 'status-mode-menu-item-active' : ''" @click="selectStatusViewMode('reading')"><Eye class="h-4 w-4" />阅读视图<Check v-if="viewMode === 'reading'" class="ml-auto h-4 w-4" /></button>
          <button :class="viewMode === 'source' ? 'status-mode-menu-item-active' : ''" @click="selectStatusViewMode('source')"><Code2 class="h-4 w-4" />源码模式<Check v-if="viewMode === 'source'" class="ml-auto h-4 w-4" /></button>
          <button :class="viewMode === 'writing' ? 'status-mode-menu-item-active' : ''" @click="selectStatusViewMode('writing')"><PenLine class="h-4 w-4" />实时阅览<Check v-if="viewMode === 'writing'" class="ml-auto h-4 w-4" /></button>
        </div>
      </div>
      <span>{{ wordCount }} 个词</span>
      <span>{{ noteContent.length }} 个字符</span>
      <span class="status-sync" :class="isSynced ? 'status-synced' : 'status-saving'" :title="isSynced ? '已保存' : '正在保存'">
        <Save v-if="isSynced" class="h-4 w-4" />
        <CloudUpload v-else class="h-4 w-4" />
      </span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  AlignCenter, AlignJustify, AlignLeft, AlignRight, ArrowUpDown, Bot, ChevronDown, ChevronRight, ChevronUp,
  ChevronsDown, ChevronsUp, CircleArrowUp, CirclePlus, Clock3, CloudUpload, Code2, Download, Eraser, Eye,
  FileSearch, FileText, Folder, FolderPlus,
  Highlighter, ImagePlus, IndentDecrease, IndentIncrease, Library, Link, List, ListOrdered, ListTodo,
  History, ListTree, MessageSquare, MessageSquarePlus, Mic, Minus, MoreHorizontal, NotebookPen, NotepadText, Paintbrush, PanelLeftClose,
  PanelLeftOpen, PanelRightClose, PanelRightOpen, PanelTop, Paperclip, PenLine, Quote, Redo2,
  Save, Search, Send, Sigma, Sparkles, SquarePen, SquarePlus, Strikethrough, Table2, Tags, Trash2, Undo2, Upload,
  WandSparkles, X,
} from 'lucide-vue-next'

type SideTab = 'folders' | 'recent' | 'search'
type ViewMode = 'writing' | 'source' | 'reading'
type SortOption = 'titleAsc' | 'titleDesc' | 'updatedDesc' | 'updatedAsc' | 'createdDesc' | 'createdAsc'
type Note = {
  id: string
  title: string
  content: string
  folderId: string | null
  updatedLabel: string
  recentGroup: '今天' | '过去 7 天' | '过去 30 天'
  updatedAt: number
  createdAt: number
}

const SIDE_PANEL_MIN_WIDTH = 248
const SIDE_PANEL_MAX_WIDTH = 430
const RIGHT_PANEL_MIN_WIDTH = 248
const RIGHT_PANEL_MAX_WIDTH = 430

const sideTab = ref<SideTab>('folders')
const sidePanelVisible = ref(true)
const sidePanelWidth = ref(318)
const rightPanelVisible = ref(true)
const rightPanelWidth = ref(320)
const isSidePanelResizing = ref(false)
const isRightPanelResizing = ref(false)
const rightTab = ref('ai')
const viewMode = ref<ViewMode>('writing')
const activeNoteId = ref('architecture')
const searchKeyword = ref('')
const searchSort = ref<SortOption>('updatedDesc')
const showSearchSortMenu = ref(false)
const searchSortMenuRef = ref<HTMLElement | null>(null)
const collapsedFolders = ref<string[]>([])
const showMoreMenu = ref(false)
const moreMenuRef = ref<HTMLElement | null>(null)
const showInsertMenu = ref(false)
const insertMenuRef = ref<HTMLElement | null>(null)
const showHighlightMenu = ref(false)
const highlightMenuRef = ref<HTMLElement | null>(null)
const showTextColorMenu = ref(false)
const textColorMenuRef = ref<HTMLElement | null>(null)
const showBlockMenu = ref(false)
const blockMenuRef = ref<HTMLElement | null>(null)
const showAlignMenu = ref(false)
const alignMenuRef = ref<HTMLElement | null>(null)
const showFindPanel = ref(false)
const showStatusModeMenu = ref(false)
const statusModeMenuRef = ref<HTMLElement | null>(null)
const isSynced = ref(true)
let syncTimer: ReturnType<typeof setTimeout> | null = null
const showRecentFilterMenu = ref(false)
const recentFilterMenuRef = ref<HTMLElement | null>(null)
const recentFolderFilter = ref<'all' | string>('all')
const recentSortOption = ref<SortOption>('updatedDesc')
const showRecentSortMenu = ref(false)
const recentSortMenuRef = ref<HTMLElement | null>(null)
const showSortMenu = ref(false)
const sortMenuRef = ref<HTMLElement | null>(null)
const sortOption = ref<SortOption>('titleAsc')
const richEditorRef = ref<HTMLElement | null>(null)
const sourceEditorRef = ref<HTMLTextAreaElement | null>(null)
const aiInput = ref('')
const aiConversationRef = ref<HTMLElement | null>(null)
const activeSessionId = ref('session-1')
const conversationSessions = ref([
  { id: 'session-1', label: '1' },
  { id: 'session-2', label: '2' },
])
const conversationMessages = ref<Record<string, { id: string; role: 'user' | 'assistant'; content: string }[]>>({
  'session-1': [
    { id: 'intro', role: 'assistant', content: '我已读取当前笔记。可以基于内容帮你总结、解释、润色，或补充结构化内容。' },
    { id: 'question', role: 'user', content: '请帮我梳理这篇笔记的核心链路。' },
    { id: 'answer', role: 'assistant', content: '核心链路是：用低门槛编辑器沉淀 Markdown 笔记，再按需同步进入知识库，经过分块和向量化后用于 RAG 问答。' },
  ],
  'session-2': [
    { id: 'session-two', role: 'assistant', content: '这是第二个会话框。它与第一个会话的上下文独立。' },
  ],
})
const showConversationHistory = ref(false)
const conversationHistoryRef = ref<HTMLElement | null>(null)
const conversationHistory = [
  { id: 'history-1', title: '梳理 NoteMind 的核心链路', time: '刚刚' },
  { id: 'history-2', title: '生成数据库设计说明', time: '昨天' },
  { id: 'history-3', title: '解释 RAG 文档分块策略', time: '3 天前' },
]
const selectedModel = ref('DeepSeek V4 Flash[1m]')
const models = ['DeepSeek V4 Flash[1m]', 'Opus 1M', 'Sonnet 1M', 'Haiku']
const showModelMenu = ref(false)
const modelMenuRef = ref<HTMLElement | null>(null)
const thinkingMode = ref('High')
const thinkingModes = ['Ultra', 'High', 'Med', 'Low', 'Off']
const showThinkingMenu = ref(false)
const thinkingMenuRef = ref<HTMLElement | null>(null)

const sideTabs = [
  { key: 'folders', icon: ListTree, label: '文件夹' },
  { key: 'recent', icon: Clock3, label: '最近编辑' },
  { key: 'search', icon: Search, label: '搜索' },
] as const

const sortOptions = [
  { value: 'titleAsc', label: '文件名（A-Z）', dividerAfter: false },
  { value: 'titleDesc', label: '文件名（Z-A）', dividerAfter: true },
  { value: 'updatedDesc', label: '编辑时间（从新到旧）', dividerAfter: false },
  { value: 'updatedAsc', label: '编辑时间（从旧到新）', dividerAfter: true },
  { value: 'createdDesc', label: '创建时间（从新到旧）', dividerAfter: false },
  { value: 'createdAsc', label: '创建时间（从旧到新）', dividerAfter: false },
] as const

const rightTabs = [
  { key: 'backlinks', icon: Link, label: '反向链接' },
  { key: 'outgoing', icon: ArrowUpDown, label: '出链' },
  { key: 'ai', icon: Sparkles, label: 'AI 助手' },
]

const highlightColors = ['#fff0ae', '#baf0d0', '#bde5ff', '#ffd3d3', '#e6d5ff', '#ffe0ad', '#d9d9d9']
const textColors = ['#18181b', '#2563eb', '#059669', '#dc2626', '#9333ea', '#d97706', '#71717a']
const blockOptions = [
  { tag: 'h1', label: '标题' },
  { tag: 'h2', label: '标题 1' },
  { tag: 'h3', label: '标题 2' },
  { tag: 'p', label: '正文 1' },
  { tag: 'div', label: '正文 2' },
]
const alignOptions = [
  { value: 'justifyLeft', label: '左对齐', icon: AlignLeft },
  { value: 'justifyCenter', label: '居中', icon: AlignCenter },
  { value: 'justifyRight', label: '右对齐', icon: AlignRight },
  { value: 'justifyFull', label: '两端对齐', icon: AlignJustify },
]

const folders = [
  { id: 'plan', name: '开发计划' },
  { id: 'design', name: '系统设计' },
  { id: 'rag', name: 'RAG 资料' },
]

const notes = ref<Note[]>([
  {
    id: 'architecture', title: 'NoteMind 系统架构', folderId: 'design', updatedLabel: '43 分钟前', recentGroup: '今天', updatedAt: 5, createdAt: 2,
    content: '# NoteMind 系统架构\n\n## 核心定位\n\nNoteMind 是一个基于 RAG 的智能知识管理平台。笔记负责知识创造和沉淀，知识库负责检索，AI 则辅助整理和生成。\n\n## 技术选型\n\n- 前端：Vue 3 + Vite + Tailwind CSS\n- 后端：Spring Boot 3 + Spring AI\n- 数据：PostgreSQL + pgvector\n- 文件：MinIO\n\n## 核心链路\n\n用户在低门槛编辑器中创作内容，底层保存 Markdown。具体表结构见 [[数据库设计文档]]，笔记编辑体验见 [[笔记编辑器方案]]。笔记可以按需加入知识库，经过分块和向量化后，为 RAG 问答提供可靠上下文。',
  },
  {
    id: 'database', title: '数据库设计文档', folderId: 'design', updatedLabel: '今天 09:18', recentGroup: '今天', updatedAt: 4, createdAt: 5,
    content: '# 数据库设计文档\n\n## 笔记表\n\n笔记保存标题、Markdown 内容、所属文件夹和更新时间。\n\n> Markdown 是唯一内容来源，HTML 只作为渲染结果。\n\n## 关联关系\n\n使用 `[[笔记标题]]` 建立双向链接，并由后端解析维护关系图谱。',
  },
  {
    id: 'editor', title: '笔记编辑器方案', folderId: 'plan', updatedLabel: '昨天', recentGroup: '过去 7 天', updatedAt: 3, createdAt: 4,
    content: '# 笔记编辑器方案\n\n默认使用即时渲染编辑模式，让不熟悉 Markdown 的用户也能通过工具栏完成排版。\n\n需要时可以切换到源码模式，直接编辑原始 Markdown。',
  },
  {
    id: 'rag-chunk', title: 'RAG 文档分块策略', folderId: 'rag', updatedLabel: '3 天前', recentGroup: '过去 7 天', updatedAt: 2, createdAt: 3,
    content: '# RAG 文档分块策略\n\n优先按 Markdown 标题与段落切分，每个知识块保留标题路径和来源信息。\n\n- 块大小：512 tokens\n- 重叠：80 tokens\n- 支持引用溯源',
  },
  {
    id: 'welcome', title: '欢迎使用 NoteMind', folderId: null, updatedLabel: '7 月 21 日', recentGroup: '过去 30 天', updatedAt: 1, createdAt: 1,
    content: '# 欢迎使用 NoteMind\n\n从一篇笔记开始，逐步建立自己的知识系统。',
  },
])

const activeNote = computed(() => notes.value.find((note) => note.id === activeNoteId.value) || notes.value[0])
const noteContent = ref(activeNote.value.content)
const outgoingNotes = computed(() => linkedNotesFor(activeNote.value))
const backlinkNotes = computed(() => notes.value.filter((note) => note.id !== activeNote.value.id && note.content.includes(`[[${activeNote.value.title}]]`)))
const activeConversationMessages = computed(() => conversationMessages.value[activeSessionId.value] || [])
const searchResults = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return []
  const matchedNotes = notes.value.filter((note) => `${note.title}\n${note.content}`.toLowerCase().includes(keyword))
  return matchedNotes.sort((left, right) => compareNotesBy(searchSort.value, left, right))
})
const recentFilterLabel = computed(() => recentFolderFilter.value === 'all'
  ? '全部'
  : folderName(recentFolderFilter.value))
const searchSortLabel = computed(() => sortOptions.find((option) => option.value === searchSort.value)?.label || '')
const recentGroups = computed(() => ['今天', '过去 7 天', '过去 30 天'].map((label) => ({
  label,
  notes: notes.value
    .filter((note) => note.recentGroup === label && (recentFolderFilter.value === 'all' || note.folderId === recentFolderFilter.value))
    .sort((left, right) => compareNotesBy(recentSortOption.value, left, right)),
})).filter((group) => group.notes.length))
const allFoldersCollapsed = computed(() => folders.length > 0 && folders.every((folder) => collapsedFolders.value.includes(folder.id)))
const wordCount = computed(() => {
  const text = noteContent.value
  const chineseCount = (text.match(/[\u4e00-\u9fff]/g) || []).length
  const otherWords = text.replace(/[\u4e00-\u9fff]/g, ' ').match(/[A-Za-z0-9_]+/g) || []
  return chineseCount + otherWords.length
})
const backlinkCount = computed(() => backlinkNotes.value.length)
const outline = computed(() => noteContent.value.split('\n').filter((line) => /^#{1,3}\s/.test(line)).map((line) => line.replace(/^#+\s/, '')))
const renderedNoteContent = computed(() => renderMarkdown(noteContent.value))

function notesInFolder(folderId: string | null) {
  return notes.value.filter((note) => note.folderId === folderId).sort(compareNotes)
}

function folderName(folderId: string | null) {
  return folders.find((folder) => folder.id === folderId)?.name || '我的笔记'
}

function linkedNotesFor(note: Note) {
  const titles = [...note.content.matchAll(/\[\[([^\]]+)\]\]/g)].map((match) => match[1])
  return titles
    .map((title) => notes.value.find((candidate) => candidate.title === title))
    .filter((candidate): candidate is Note => Boolean(candidate))
    .filter((candidate, index, list) => list.findIndex((item) => item.id === candidate.id) === index)
}

function excerptFor(note: Note) {
  return note.content
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/[>*_`#[\]()]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 88)
}

function searchExcerptFor(note: Note) {
  const content = note.content
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/[>*_`#[\]()]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  const keyword = searchKeyword.value.trim().toLowerCase()
  const index = content.toLowerCase().indexOf(keyword)
  if (index < 0) return content.slice(0, 96)
  const start = Math.max(0, index - 32)
  const end = Math.min(content.length, index + keyword.length + 64)
  return `${start ? '...' : ''}${content.slice(start, end)}${end < content.length ? '...' : ''}`
}

function searchMatchCount(note: Note) {
  const text = `${note.title}\n${note.content}`.toLowerCase()
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return 0
  let count = 0
  let index = text.indexOf(keyword)
  while (index !== -1) {
    count += 1
    index = text.indexOf(keyword, index + keyword.length)
  }
  return count
}

function highlightSearchText(value: string) {
  const escapedValue = escapeHtml(value)
  const keyword = searchKeyword.value.trim()
  if (!keyword) return escapedValue
  const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escapedValue.replace(new RegExp(`(${escapedKeyword})`, 'gi'), '<mark>$1</mark>')
}

function selectNote(noteId: string) {
  activeNoteId.value = noteId
  noteContent.value = activeNote.value.content
  nextTick(() => {
    loadRichEditor()
    if (viewMode.value === 'writing') richEditorRef.value?.focus()
    else if (viewMode.value === 'source') sourceEditorRef.value?.focus()
  })
}

function openSearchResult(noteId: string) {
  selectNote(noteId)
  viewMode.value = 'writing'
}

function addConversationSession() {
  const id = `session-${Date.now()}`
  conversationSessions.value.push({ id, label: String(conversationSessions.value.length + 1) })
  conversationMessages.value[id] = [{ id: `welcome-${id}`, role: 'assistant', content: '这是一个新的会话框，可以基于当前笔记开始交流。' }]
  activeSessionId.value = id
}

function startNewConversation() {
  conversationMessages.value[activeSessionId.value] = [{ id: `new-${Date.now()}`, role: 'assistant', content: '已开始新的对话。当前笔记仍会作为上下文提供给 AI。' }]
}

function restoreConversation(item: { id: string; title: string }) {
  conversationMessages.value[activeSessionId.value] = [
    { id: `${item.id}-user`, role: 'user', content: item.title },
    { id: `${item.id}-assistant`, role: 'assistant', content: '已恢复这段历史对话。你可以继续围绕当前笔记提问。' },
  ]
  showConversationHistory.value = false
  nextTick(() => scrollAiConversation('bottom'))
}

function sendAiMessage() {
  const message = aiInput.value.trim()
  if (!message) return
  const messages = conversationMessages.value[activeSessionId.value] || []
  messages.push({ id: `user-${Date.now()}`, role: 'user', content: message })
  messages.push({ id: `assistant-${Date.now()}`, role: 'assistant', content: '这是 AI 助手原型回复。后续会通过流式接口，结合当前笔记和所选模型生成真实回答。' })
  conversationMessages.value[activeSessionId.value] = messages
  aiInput.value = ''
  nextTick(() => scrollAiConversation('bottom'))
}

function scrollAiConversation(direction: 'top' | 'previous' | 'next' | 'bottom') {
  const container = aiConversationRef.value
  if (!container) return
  if (direction === 'top') container.scrollTo({ top: 0, behavior: 'smooth' })
  else if (direction === 'bottom') container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' })
  else container.scrollBy({ top: direction === 'previous' ? -160 : 160, behavior: 'smooth' })
}

function selectModel(model: string) {
  selectedModel.value = model
  showModelMenu.value = false
}

function selectThinkingMode(mode: string) {
  thinkingMode.value = mode
  showThinkingMenu.value = false
}

function toggleFolder(folderId: string) {
  collapsedFolders.value = collapsedFolders.value.includes(folderId)
    ? collapsedFolders.value.filter((id) => id !== folderId)
    : [...collapsedFolders.value, folderId]
}

function toggleAllFolders() {
  collapsedFolders.value = allFoldersCollapsed.value ? [] : folders.map((folder) => folder.id)
}

function revealActiveNote() {
  if (activeNote.value.folderId) {
    collapsedFolders.value = collapsedFolders.value.filter((id) => id !== activeNote.value.folderId)
  }
  nextTick(() => {
    const selector = `[data-note-id="${activeNoteId.value}"]`
    document.querySelector<HTMLElement>(selector)?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  })
}

function createPrototypeNote() {
  const id = `note-${Date.now()}`
  notes.value.unshift({
    id, title: '无标题笔记', content: '', folderId: null, updatedLabel: '刚刚', recentGroup: '今天', createdAt: Date.now(), updatedAt: Date.now(),
  })
  selectNote(id)
}

function touchActiveNote() {
  activeNote.value.content = noteContent.value
  activeNote.value.updatedLabel = '刚刚'
  activeNote.value.recentGroup = '今天'
  activeNote.value.updatedAt = Date.now()
  isSynced.value = false
  if (syncTimer) clearTimeout(syncTimer)
  syncTimer = setTimeout(() => { isSynced.value = true }, 700)
}

function compareNotes(left: Note, right: Note) {
  return compareNotesBy(sortOption.value, left, right)
}

function compareNotesBy(option: SortOption, left: Note, right: Note) {
  switch (option) {
    case 'titleAsc': return left.title.localeCompare(right.title, 'zh-CN')
    case 'titleDesc': return right.title.localeCompare(left.title, 'zh-CN')
    case 'updatedDesc': return right.updatedAt - left.updatedAt
    case 'updatedAsc': return left.updatedAt - right.updatedAt
    case 'createdDesc': return right.createdAt - left.createdAt
    case 'createdAsc': return left.createdAt - right.createdAt
  }
}

function selectSortOption(option: typeof sortOption.value) {
  sortOption.value = option
  showSortMenu.value = false
}

function selectRecentFolder(folderId: string) {
  recentFolderFilter.value = folderId
  showRecentFilterMenu.value = false
}

function selectRecentSortOption(option: SortOption) {
  recentSortOption.value = option
  showRecentSortMenu.value = false
}

function selectSearchSort(option: SortOption) {
  searchSort.value = option
  showSearchSortMenu.value = false
}

function setViewMode(mode: ViewMode) {
  if (viewMode.value === 'writing') syncRichEditor()
  viewMode.value = mode
  if (mode === 'writing') nextTick(loadRichEditor)
}

function selectStatusViewMode(mode: ViewMode) {
  setViewMode(mode)
  showStatusModeMenu.value = false
}

function runRichCommand(command: string, value?: string) {
  if (viewMode.value !== 'writing') {
    setViewMode('writing')
    nextTick(() => runRichCommand(command, value))
    return
  }
  richEditorRef.value?.focus()
  document.execCommand(command, false, value)
  syncRichEditor()
}

function formatRichBlock(tag: string) {
  runRichCommand('formatBlock', tag)
}

function selectBlockFormat(tag: string) {
  formatRichBlock(tag)
  showBlockMenu.value = false
}

function setRichAlignment(command: string) {
  runRichCommand(command)
  showAlignMenu.value = false
}

function applyRichColor(command: string, color: string) {
  runRichCommand(command, color)
  showHighlightMenu.value = false
  showTextColorMenu.value = false
}

function insertTodo() {
  if (viewMode.value !== 'writing') {
    setViewMode('writing')
    nextTick(insertTodo)
    return
  }
  richEditorRef.value?.focus()
  document.execCommand('insertHTML', false, '<p><input type="checkbox" contenteditable="false"> 待办事项</p>')
  syncRichEditor()
}

function insertHorizontalRule() {
  if (viewMode.value !== 'writing') {
    setViewMode('writing')
    nextTick(insertHorizontalRule)
    return
  }
  richEditorRef.value?.focus()
  document.execCommand('insertHorizontalRule')
  showInsertMenu.value = false
  syncRichEditor()
}

function insertRichLink() {
  const url = window.prompt('输入链接地址')
  if (url) runRichCommand('createLink', url)
  showInsertMenu.value = false
}

function insertWikiLink() {
  const target = notes.value.find((note) => note.id !== activeNoteId.value)
  if (!target) return
  if (viewMode.value !== 'writing') {
    setViewMode('writing')
    nextTick(insertWikiLink)
    return
  }
  richEditorRef.value?.focus()
  document.execCommand('insertHTML', false, `<span class="wikilink-chip" contenteditable="false">${escapeHtml(target.title)}</span>&nbsp;`)
  showInsertMenu.value = false
  syncRichEditor()
}

function syncRichEditor() {
  if (!richEditorRef.value) return
  noteContent.value = richHtmlToMarkdown(richEditorRef.value.innerHTML)
  touchActiveNote()
}

function loadRichEditor() {
  if (richEditorRef.value) richEditorRef.value.innerHTML = markdownToRichHtml(noteContent.value)
}

function escapeHtml(value: string) {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function markdownInlineToHtml(value: string) {
  return escapeHtml(value)
    .replace(/\[\[(.*?)\]\]/g, '<span class="wikilink-chip" contenteditable="false">$1</span>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/~~(.*?)~~/g, '<s>$1</s>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}

function markdownToRichHtml(value: string) {
  const lines = value.split('\n')
  const blocks: string[] = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]
    if (!line.trim()) {
      index += 1
      continue
    }
    const heading = line.match(/^(#{1,3})\s+(.*)$/)
    if (heading) {
      const level = heading[1].length
      blocks.push(`<h${level}>${markdownInlineToHtml(heading[2])}</h${level}>`)
      index += 1
      continue
    }
    if (line.startsWith('- ')) {
      const items: string[] = []
      while (index < lines.length && lines[index].startsWith('- ')) {
        items.push(`<li>${markdownInlineToHtml(lines[index].slice(2))}</li>`)
        index += 1
      }
      blocks.push(`<ul>${items.join('')}</ul>`)
      continue
    }
    if (/^\d+\.\s/.test(line)) {
      const items: string[] = []
      while (index < lines.length && /^\d+\.\s/.test(lines[index])) {
        items.push(`<li>${markdownInlineToHtml(lines[index].replace(/^\d+\.\s/, ''))}</li>`)
        index += 1
      }
      blocks.push(`<ol>${items.join('')}</ol>`)
      continue
    }
    if (line.startsWith('> ')) {
      blocks.push(`<blockquote>${markdownInlineToHtml(line.slice(2))}</blockquote>`)
      index += 1
      continue
    }
    blocks.push(`<p>${markdownInlineToHtml(line)}</p>`)
    index += 1
  }
  return blocks.join('')
}

function richHtmlToMarkdown(html: string) {
  const documentNode = new DOMParser().parseFromString(html, 'text/html')
  const serialize = (node: Node): string => {
    if (node.nodeType === Node.TEXT_NODE) return node.textContent || ''
    if (node.nodeType !== Node.ELEMENT_NODE) return ''
    const element = node as HTMLElement
    const content = Array.from(element.childNodes).map(serialize).join('')
    if (element.classList.contains('wikilink-chip')) return `[[${element.textContent || ''}]]`

    switch (element.tagName) {
      case 'BR': return '\n'
      case 'H1': return `# ${content.trim()}\n\n`
      case 'H2': return `## ${content.trim()}\n\n`
      case 'H3': return `### ${content.trim()}\n\n`
      case 'P':
      case 'DIV': return `${content.trim()}\n\n`
      case 'STRONG':
      case 'B': return `**${content}**`
      case 'EM':
      case 'I': return `*${content}*`
      case 'S':
      case 'STRIKE': return `~~${content}~~`
      case 'CODE': return `\`${content}\``
      case 'A': return `[${content}](${element.getAttribute('href') || ''})`
      case 'LI': return content
      case 'UL': return Array.from(element.children).map((item) => `- ${serialize(item).trim()}`).join('\n') + '\n\n'
      case 'OL': return Array.from(element.children).map((item, itemIndex) => `${itemIndex + 1}. ${serialize(item).trim()}`).join('\n') + '\n\n'
      case 'BLOCKQUOTE': return `> ${content.trim()}\n\n`
      case 'PRE': return `\`\`\`\n${element.textContent?.trim() || ''}\n\`\`\`\n\n`
      default: return content
    }
  }
  return Array.from(documentNode.body.childNodes).map(serialize).join('').replace(/\n{3,}/g, '\n\n').trim()
}

function renderMarkdown(value: string) {
  return escapeHtml(value)
    .replace(/^### (.*)$/gm, '<h3>$1</h3>')
    .replace(/^## (.*)$/gm, '<h2>$1</h2>')
    .replace(/^# (.*)$/gm, '<h1>$1</h1>')
    .replace(/^> (.*)$/gm, '<blockquote>$1</blockquote>')
    .replace(/^- (.*)$/gm, '<li>$1</li>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n{2,}/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>')
}

let stopSideResize: (() => void) | null = null
let stopRightResize: (() => void) | null = null

function startSidePanelResize(event: MouseEvent) {
  isSidePanelResizing.value = true
  const startX = event.clientX
  const startWidth = sidePanelWidth.value
  const onMove = (moveEvent: MouseEvent) => {
    sidePanelWidth.value = Math.min(SIDE_PANEL_MAX_WIDTH, Math.max(SIDE_PANEL_MIN_WIDTH, startWidth + moveEvent.clientX - startX))
  }
  const stop = () => {
    isSidePanelResizing.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', stop)
    stopSideResize = null
  }
  stopSideResize = stop
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', stop)
}

function startRightPanelResize(event: MouseEvent) {
  isRightPanelResizing.value = true
  const startX = event.clientX
  const startWidth = rightPanelWidth.value
  const onMove = (moveEvent: MouseEvent) => {
    rightPanelWidth.value = Math.min(RIGHT_PANEL_MAX_WIDTH, Math.max(RIGHT_PANEL_MIN_WIDTH, startWidth - (moveEvent.clientX - startX)))
  }
  const stop = () => {
    isRightPanelResizing.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', stop)
    stopRightResize = null
  }
  stopRightResize = stop
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', stop)
}

function handleOutsideClick(event: MouseEvent) {
  if (moreMenuRef.value && !moreMenuRef.value.contains(event.target as Node)) showMoreMenu.value = false
  if (insertMenuRef.value && !insertMenuRef.value.contains(event.target as Node)) showInsertMenu.value = false
  if (highlightMenuRef.value && !highlightMenuRef.value.contains(event.target as Node)) showHighlightMenu.value = false
  if (textColorMenuRef.value && !textColorMenuRef.value.contains(event.target as Node)) showTextColorMenu.value = false
  if (blockMenuRef.value && !blockMenuRef.value.contains(event.target as Node)) showBlockMenu.value = false
  if (alignMenuRef.value && !alignMenuRef.value.contains(event.target as Node)) showAlignMenu.value = false
  if (statusModeMenuRef.value && !statusModeMenuRef.value.contains(event.target as Node)) showStatusModeMenu.value = false
  if (conversationHistoryRef.value && !conversationHistoryRef.value.contains(event.target as Node)) showConversationHistory.value = false
  if (modelMenuRef.value && !modelMenuRef.value.contains(event.target as Node)) showModelMenu.value = false
  if (thinkingMenuRef.value && !thinkingMenuRef.value.contains(event.target as Node)) showThinkingMenu.value = false
  if (sortMenuRef.value && !sortMenuRef.value.contains(event.target as Node)) showSortMenu.value = false
  if (recentFilterMenuRef.value && !recentFilterMenuRef.value.contains(event.target as Node)) showRecentFilterMenu.value = false
  if (recentSortMenuRef.value && !recentSortMenuRef.value.contains(event.target as Node)) showRecentSortMenu.value = false
  if (searchSortMenuRef.value && !searchSortMenuRef.value.contains(event.target as Node)) showSearchSortMenu.value = false
}

watch(activeNoteId, () => {
  noteContent.value = activeNote.value.content
  nextTick(loadRichEditor)
})
watch(viewMode, (mode) => {
  if (mode === 'writing') nextTick(loadRichEditor)
})
onMounted(() => {
  document.addEventListener('mousedown', handleOutsideClick)
  nextTick(loadRichEditor)
})
onBeforeUnmount(() => {
  stopSideResize?.()
  stopRightResize?.()
  if (syncTimer) clearTimeout(syncTimer)
  document.removeEventListener('mousedown', handleOutsideClick)
})
</script>

<style scoped>
.notebook-shell { position: relative; --mint: #e8f5ee; --mint-strong: #3c9270; }

.side-tab, .icon-button, .mode-button, .format-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: background-color .15s ease, color .15s ease;
}
.side-tab, .icon-button, .mode-button { height: 30px; width: 30px; }
.side-tab:hover, .icon-button:hover, .mode-button:hover, .format-button:hover { background: #f0f0ee; color: #27272a; }
.side-tab-active, .mode-button-active { background: var(--mint); color: #247456; }
.file-action-bar { display: flex; height: 54px; flex-shrink: 0; align-items: center; justify-content: center; gap: 6px; border-bottom: 1px solid #f0f0ee; }
.file-action-active { background: #e9e9e7; color: #27272a; }
.sort-menu { position: absolute; z-index: 45; top: calc(100% + 7px); left: -10px; width: 224px; overflow: hidden; border: 1px solid #e2e2e2; border-radius: 8px; background: #fff; padding: 5px 0; box-shadow: 0 10px 26px rgba(24, 24, 27, .14); }
.sort-menu-item { display: flex; width: 100%; align-items: center; justify-content: space-between; padding: 8px 14px; text-align: left; font-size: 14px; color: #3f3f46; }
.sort-menu-item:hover, .sort-menu-item-active { background: #f3f3f1; }.sort-menu-item-active { color: #18181b; }
.sort-menu-divider { height: 1px; margin: 5px 0; background: #e9e9e7; }
.side-panel-resizer { position: absolute; z-index: 20; top: 0; right: -3px; height: 100%; width: 6px; cursor: col-resize; }
.right-resizer { right: auto; left: -3px; }
.side-panel-resizer:hover { background: rgba(60, 146, 112, .2); }
.folder-heading { display: flex; width: 100%; align-items: center; gap: 7px; border-radius: 6px; padding: 7px 8px; text-align: left; font-size: 14px; font-weight: 500; color: #52525b; }
.folder-heading:hover { background: #f2f2f0; }
.tree-note-row { display: flex; width: 100%; align-items: center; gap: 8px; border-radius: 6px; padding: 7px 10px 7px 30px; text-align: left; font-size: 14px; color: #52525b; }
.tree-note-row:hover, .tree-note-row-active { background: var(--mint); color: #235f49; }
.recent-note-row, .search-result-row { display: flex; width: 100%; flex-direction: column; border-radius: 7px; padding: 10px 11px; text-align: left; transition: background-color .15s ease; }
.recent-note-row:hover, .recent-note-row-active, .search-result-row:hover { background: var(--mint); }
.recent-folder-link { display: inline-flex; width: fit-content; align-items: center; gap: 6px; margin-top: 8px; border-radius: 4px; padding: 2px 4px; color: #a1a1aa; font-size: 12px; line-height: 1.25; transition: color .15s ease, background-color .15s ease; }
.recent-folder-link:hover { background: rgba(255, 255, 255, .7); color: #287756; }
.recent-view-toolbar { display: flex; align-items: center; justify-content: space-between; margin: 1px 0 20px 2px; }
.recent-filter { display: inline-flex; align-items: center; gap: 4px; border-radius: 6px; padding: 7px 9px; font-size: 16px; font-weight: 700; color: #18181b; }
.recent-filter:hover, .recent-filter-active { background: #f1f1ef; color: #18181b; }
.recent-filter-menu { position: absolute; z-index: 45; top: 38px; left: -4px; width: 246px; overflow: hidden; border-radius: 12px; background: #fff; padding: 6px 0; box-shadow: 0 12px 26px rgba(24, 24, 27, .14); }
.recent-filter-menu-item { display: flex; width: 100%; align-items: center; gap: 11px; padding: 9px 14px; text-align: left; font-size: 14px; font-weight: 500; color: #27272a; }
.recent-filter-menu-item:hover, .recent-filter-menu-item-active { background: #f5faf7; }.recent-filter-menu-item-active { color: #1f7253; }
.recent-filter-menu-divider { height: 1px; margin: 4px 0; background: #eeeeec; }.recent-folder-menu-item { align-items: flex-start; }
.recent-sort-menu { top: calc(100% + 6px); right: 0; left: auto; }
.search-input { width: 100%; height: 38px; border: 1px solid #d4d4d8; border-radius: 7px; background: #fff; padding: 0 38px 0 34px; outline: none; font-size: 14px; color: #3f3f46; }
.search-input:focus { border-color: #8ccbb1; box-shadow: 0 0 0 3px rgba(120, 192, 156, .13); }
.search-sort-button { display: inline-flex; min-width: 130px; align-items: center; justify-content: space-between; gap: 6px; border: 1px solid #dedee0; border-radius: 6px; background: #fff; padding: 5px 7px; font-size: 12px; color: #52525b; }.search-sort-button:hover { border-color: #bcbcc1; color: #27272a; }
.search-sort-menu { position: absolute; z-index: 45; top: calc(100% + 4px); right: 0; width: 206px; overflow: hidden; border: 1px solid #a1a1aa; background: #fff; padding: 3px 0; box-shadow: 0 8px 22px rgba(24,24,27,.12); }.search-sort-menu-item { display: flex; width: 100%; align-items: center; justify-content: space-between; padding: 7px 10px; text-align: left; font-size: 13px; color: #27272a; }.search-sort-menu-item:hover, .search-sort-menu-item-active { background: #e8f5ee; color: #1f7253; }.search-sort-menu-divider { height: 1px; margin: 3px 0; background: #e5e5e5; }
.search-empty-state, .search-start-state { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 28px; padding: 28px 12px; color: #a1a1aa; font-size: 13px; line-height: 1.6; text-align: center; }.search-empty-state { display: block; }
.search-result-row :deep(mark) { border-radius: 2px; background: #fff0ae; padding: 0 1px; color: inherit; }
.empty-state { border: 1px dashed #e4e4e7; border-radius: 6px; padding: 14px; font-size: 13px; line-height: 1.6; color: #a1a1aa; }
.editor-toolbar { display: flex; height: 50px; shrink: 0; align-items: center; gap: 2px; border-bottom: 1px solid #e8e8e8; background: #fff; padding: 0 18px; }
.format-button { height: 28px; width: 28px; font-size: 14px; }
.format-select { display: inline-flex; height: 28px; align-items: center; gap: 4px; border-radius: 5px; padding: 0 8px; font-size: 12px; color: #52525b; }
.format-select:hover, .format-select-active, .format-button-active { background: #f0f0ee; color: #27272a; }
.toolbar-divider { height: 18px; width: 1px; margin: 0 5px; background: #e4e4e7; }
.text-color-button { position: relative; font-family: Georgia, serif; font-size: 17px; }.text-color-button::after { position: absolute; right: 6px; bottom: 4px; left: 6px; height: 2px; background: #35a878; content: ''; }
.insert-menu, .block-menu, .align-menu, .color-menu { position: absolute; z-index: 50; top: calc(100% + 7px); left: 0; background: #fff; box-shadow: 0 12px 28px rgba(24, 24, 27, .14); }
.insert-menu { width: 196px; overflow: hidden; border-radius: 16px; padding: 8px 0; }.insert-menu-item { display: flex; width: 100%; align-items: center; gap: 12px; padding: 8px 15px; text-align: left; color: #3f3f46; font-size: 14px; }.insert-menu-item:hover { background: #f3f8f5; color: #247456; }.toolbar-menu-divider { height: 1px; margin: 5px 12px; background: #e8e8e8; }
.color-menu { width: 280px; border-radius: 16px; padding: 14px 16px; }.color-menu p { margin-bottom: 10px; color: #71717a; font-size: 13px; }.color-reset { width: 100%; border: 1px solid #e4e4e7; border-radius: 6px; padding: 7px; color: #52525b; font-size: 13px; }.color-reset:hover { background: #f6f6f5; }.color-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; margin-top: 12px; }.color-grid button { width: 28px; height: 24px; border-radius: 3px; transition: transform .12s ease; }.color-grid button:hover { outline: 2px solid #3c9270; outline-offset: 1px; transform: scale(1.05); }
.block-menu { width: 150px; overflow: hidden; border-radius: 14px; padding: 8px; }.block-menu button { display: block; width: 100%; border-radius: 6px; padding: 6px 9px; text-align: left; color: #27272a; }.block-menu button:hover { background: #e8f5ee; color: #247456; }.block-menu-h1 { font-size: 25px; font-weight: 700; }.block-menu-h2 { font-size: 21px; font-weight: 700; }.block-menu-h3 { font-size: 17px; font-weight: 700; }.block-menu-p, .block-menu-div { font-size: 14px; }
.align-menu { width: 56px; overflow: hidden; border-radius: 14px; padding: 7px; }.align-menu button { display: flex; width: 100%; height: 30px; align-items: center; justify-content: center; border-radius: 5px; color: #52525b; }.align-menu button:hover { background: #e8f5ee; color: #247456; }
.find-panel { display: flex; align-items: center; gap: 8px; border-bottom: 1px solid #e8e8e8; background: #fafafa; padding: 8px 20px; }.find-panel input { width: 260px; border: 0; background: transparent; outline: none; color: #3f3f46; font-size: 13px; }.find-panel-arrow { display: inline-flex; height: 26px; width: 26px; align-items: center; justify-content: center; border-radius: 5px; color: #71717a; }.find-panel-arrow:hover { background: #eee; color: #27272a; }
.editor-page { width: min(100%, 900px); margin: 0 auto; padding: 58px 48px 100px; }
.title-input { width: 100%; border: 0; background: transparent; outline: none; font-size: 34px; line-height: 1.25; font-weight: 700; color: #18181b; }
.title-input::placeholder { color: #d4d4d8; }
.rich-editor { min-height: 620px; margin-top: 30px; outline: none; color: #3f3f46; font-size: 16px; line-height: 1.9; }
.rich-editor:empty::before { content: attr(data-placeholder); color: #c4c4c8; pointer-events: none; }
.rich-editor :deep(h1), .rich-editor :deep(h2), .rich-editor :deep(h3) { color: #1f2933; font-weight: 700; }.rich-editor :deep(h1) { margin: 30px 0 14px; font-size: 28px; }.rich-editor :deep(h2) { margin: 27px 0 12px; font-size: 22px; }.rich-editor :deep(h3) { margin: 22px 0 10px; font-size: 18px; }
.rich-editor :deep(p) { margin: 0 0 15px; }.rich-editor :deep(ul), .rich-editor :deep(ol) { margin: 0 0 17px 24px; }.rich-editor :deep(li) { padding-left: 4px; }.rich-editor :deep(blockquote) { margin: 18px 0; border-left: 3px solid #8fc9b0; padding-left: 14px; color: #6b7280; }.rich-editor :deep(pre) { margin: 16px 0; border-radius: 6px; background: #f4f5f4; padding: 14px; font-family: Consolas, "Cascadia Code", monospace; font-size: 13px; line-height: 1.6; }
.rich-editor :deep(code) { border-radius: 4px; background: #f1f1ef; padding: 2px 5px; font-family: Consolas, monospace; font-size: 13px; }.rich-editor :deep(.wikilink-chip) { display: inline-flex; align-items: center; border-radius: 4px; background: #e8f5ee; padding: 1px 5px; color: #267653; font-size: .92em; }
.source-editor { display: block; width: 100%; min-height: 620px; margin-top: 30px; resize: none; border: 1px solid #e7e7e7; border-radius: 6px; background: #fafafa; padding: 16px; outline: none; color: #3f3f46; font-family: Consolas, "Cascadia Code", monospace; font-size: 14px; line-height: 1.75; }
.reading-content { margin-top: 30px; color: #3f3f46; font-size: 16px; line-height: 1.9; }
.reading-content :deep(h1) { margin: 28px 0 12px; font-size: 27px; color: #18181b; }
.reading-content :deep(h2) { margin: 24px 0 10px; font-size: 21px; color: #27272a; }
.reading-content :deep(h3) { margin: 20px 0 8px; font-size: 17px; color: #3f3f46; }
.reading-content :deep(p) { margin: 0 0 14px; }
.reading-content :deep(li) { margin-left: 20px; }
.reading-content :deep(blockquote) { margin: 14px 0; border-left: 3px solid #94cdb5; padding-left: 14px; color: #71717a; }
.reading-content :deep(code) { border-radius: 4px; background: #f1f1ef; padding: 2px 5px; font-family: Consolas, monospace; font-size: 13px; }
.more-menu { position: absolute; z-index: 40; top: calc(100% + 6px); right: 0; width: 166px; border: 1px solid #e4e4e7; border-radius: 7px; background: #fff; padding: 4px; box-shadow: 0 10px 24px rgba(24,24,27,.1); }
.more-menu-item { display: flex; width: 100%; align-items: center; gap: 8px; border-radius: 5px; padding: 8px; text-align: left; font-size: 13px; color: #52525b; }
.more-menu-item:hover { background: #f4f4f5; }
.note-status-bar { position: absolute; z-index: 70; right: 0; bottom: 0; display: flex; height: 31px; align-items: center; gap: 10px; border-top: 1px solid #e4e4e7; border-left: 1px solid #e4e4e7; border-radius: 9px 0 0 0; background: rgba(255, 255, 255, .96); padding: 0 11px 0 14px; color: #71717a; font-size: 12px; box-shadow: -5px -4px 16px rgba(24, 24, 27, .04); }
.status-mode-button { display: inline-flex; height: 24px; width: 24px; align-items: center; justify-content: center; border-radius: 5px; color: #71717a; }.status-mode-button:hover { background: #f0f0ee; color: #27272a; }
.status-mode-menu { position: absolute; right: -3px; bottom: calc(100% + 7px); width: 164px; overflow: hidden; border: 1px solid #e2e2e2; border-radius: 10px; background: #fff; padding: 5px; box-shadow: 0 10px 26px rgba(24, 24, 27, .14); }.status-mode-menu button { display: flex; width: 100%; align-items: center; gap: 9px; border-radius: 6px; padding: 8px; text-align: left; color: #3f3f46; font-size: 13px; }.status-mode-menu button:hover, .status-mode-menu-item-active { background: #e8f5ee; color: #247456; }
.status-sync { display: inline-flex; align-items: center; }.status-synced { color: #3c9270; }.status-saving { color: #d98b32; }
.ai-action { display: inline-flex; height: 34px; align-items: center; justify-content: center; gap: 6px; border: 1px solid #e4e4e7; border-radius: 6px; background: #fff; color: #52525b; font-size: 12px; }
.ai-action:hover { border-color: #acd9c4; background: #f4faf6; color: #247456; }
.ai-input { width: 100%; resize: none; border: 1px solid #e4e4e7; border-radius: 7px; background: #fafafa; padding: 9px 10px; outline: none; font-size: 13px; line-height: 1.5; }
.send-button { display: inline-flex; height: 30px; width: 30px; align-items: center; justify-content: center; border-radius: 6px; background: #d4d4d8; color: white; }
.send-button-active { background: #3c9270; }
.link-note-row { display: flex; width: 100%; align-items: center; gap: 9px; border-radius: 6px; padding: 9px 8px; text-align: left; color: #52525b; font-size: 13px; }.link-note-row:hover { background: #e8f5ee; color: #247456; }
.ai-conversation-wrap { position: relative; min-height: 0; flex: 1; }.ai-conversation-scroll { height: 100%; overflow: auto; scrollbar-gutter: stable; }.ai-scroll-controls { position: absolute; top: 50%; right: 8px; display: flex; transform: translateY(-50%); flex-direction: column; gap: 7px; }.ai-scroll-controls button { display: inline-flex; height: 29px; width: 29px; align-items: center; justify-content: center; border: 1px solid #e4e4e7; border-radius: 999px; background: #fff; color: #71717a; box-shadow: 0 2px 5px rgba(24,24,27,.08); }.ai-scroll-controls button:hover { border-color: #9dceb8; background: #f2faf5; color: #247456; }
.ai-context-chip { display: inline-flex; max-width: 100%; align-items: center; gap: 5px; border-radius: 5px; background: #f0f4f2; padding: 5px 7px; color: #71717a; font-size: 11px; }.ai-message { max-width: 93%; border-radius: 8px; padding: 10px 11px; font-size: 13px; line-height: 1.65; }.ai-message p { white-space: pre-wrap; }.ai-message-role { display: block; margin-bottom: 4px; color: #a1a1aa; font-size: 11px; font-weight: 600; }.ai-message-assistant { border: 1px solid #ececec; background: #fff; color: #52525b; }.ai-message-user { margin-left: auto; background: #e8f5ee; color: #245f49; }
.ai-session-bar { position: relative; display: flex; min-height: 39px; align-items: center; border-top: 1px solid #e9e9e9; padding: 4px 9px; }.ai-session-tab { display: inline-flex; height: 25px; min-width: 25px; align-items: center; justify-content: center; border: 1px solid #d8d8dc; border-radius: 4px; color: #71717a; font-size: 12px; }.ai-session-tab-active { border-color: #8b63f6; box-shadow: inset 0 0 0 1px #8b63f6; color: #6944d5; }.conversation-history-menu { position: absolute; z-index: 60; right: -2px; bottom: calc(100% + 7px); width: 286px; max-height: 340px; overflow-y: auto; border-radius: 10px; background: #fff; padding: 8px 0; box-shadow: 0 9px 26px rgba(24,24,27,.16); }.conversation-history-menu > p { padding: 5px 14px 8px; color: #71717a; font-size: 12px; font-weight: 700; text-transform: uppercase; }.conversation-history-menu button { display: flex; width: 100%; align-items: flex-start; gap: 10px; border-top: 1px solid #f0f0f0; padding: 10px 14px; text-align: left; color: #52525b; }.conversation-history-menu button:hover { background: #f4faf6; }.conversation-history-menu strong, .conversation-history-menu small { display: block; }.conversation-history-menu strong { font-size: 13px; font-weight: 500; }.conversation-history-menu small { margin-top: 3px; color: #a1a1aa; font-size: 11px; }
.ai-compose { padding: 8px 9px 40px; border-top: 1px solid #e9e9e9; background: #fff; }.ai-compose-tools { display: flex; align-items: center; gap: 7px; margin-top: 6px; white-space: nowrap; }.ai-control-text { display: inline-flex; min-width: 0; align-items: center; gap: 4px; border-radius: 5px; padding: 4px; color: #ba684f; font-size: 11px; }.ai-control-text:hover { background: #fff3ee; }.ai-context-usage { color: #d08065; font-size: 11px; }.ai-popover { position: absolute; z-index: 62; bottom: calc(100% + 6px); left: 0; min-width: 170px; overflow: hidden; border: 1px solid #e4e4e7; border-radius: 7px; background: #fff; padding: 4px; box-shadow: 0 8px 22px rgba(24,24,27,.14); }.ai-popover button { display: block; width: 100%; border-radius: 5px; padding: 7px 8px; text-align: left; color: #52525b; font-size: 12px; }.ai-popover button:hover { background: #fff0e9; color: #b95e40; }.thinking-menu { min-width: 82px; }
.property-list { display: grid; gap: 12px; font-size: 13px; }
.property-list div { display: grid; gap: 4px; border-bottom: 1px solid #eeeeee; padding-bottom: 10px; }
.property-list dt { color: #a1a1aa; }.property-list dd { color: #52525b; }
.outline-item { display: block; width: 100%; border-radius: 5px; padding: 7px 8px; text-align: left; font-size: 13px; color: #52525b; }.outline-item:hover { background: #f2f2f0; }
@media (max-width: 1180px) { .editor-page { padding-left: 32px; padding-right: 32px; } .right-resizer + header ~ section { min-width: 0; } }
</style>
