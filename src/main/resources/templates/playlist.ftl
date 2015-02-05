<#include "header.ftl">

<style type="text/css">
    #items { 
        list-style-type: none; 
        margin: 0 0 20px 0; 
        padding: 3px; 
        width: 40%;
        background-color: #BCE6F1;
    }
    
    #items li { 
        margin: 0 3px 3px 3px; 
        padding: 0.4em; padding-left: 1.5em; padding-top: 0; 
        font-size: 1.4em; 
        height: 18px;
        overflow: hidden; 
    }
    
    #items li span { position: absolute; margin-left: -1.3em; cursor: move; }
</style>


<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>
<#include "status_messages.ftl">

<div id="playlist_container">
<#if (PLAYLIST?size > 0)>
    
    <ul id="items" class="ui-corner-all">
    <#list PLAYLIST as pe>
        <li id="pe_${pe_index}" vch:id="${pe.id}" class="ui-state-default ui-corner-all">
            <span class="ui-icon ui-icon-arrowthick-2-n-s handle"></span>
            <span style="margin-left: -0.5em; cursor:pointer" class="ui-icon ui-icon-trash" onclick="removeEntry($(this).parent())"></span>
            <span style="margin-left: 0.5em; cursor:text">${pe.video.title}</span>
        </li>
    </#list>    
    </ul>
    
    <a id="play_pl" href="${ACTION}?action=play">${I18N_PLAY}</a>
    <a id="clear_pl" href="${ACTION}?action=clear">${I18N_CLEAR_PL}</a>
    
    <script type="text/javascript">
        $(function() {
            $("#items").sortable({
                handle: '.handle',
                update : function (event, ui) {
                    var order = '';
                    $('#items li').each(function() {
                        order += '&pe[]=' + $(this).attr('vch:id');
                    });
                    
                    $.ajax({
                        url: '${ACTION}?action=reorder' + order,
                        error: function() {
                            $.pnotify( {
                                pnotify_title : '${I18N_ERROR}',
                                pnotify_text : '${I18N_REORDER_FAILED}',
                                pnotify_type : 'error',
                                pnotify_hide: false
                            });
                        }
                    });
                }
            });
            $("#items").disableSelection();
        });
        
        $('#play_pl').button();
        $('#clear_pl').button();
        
        function removeEntry(li) {
            if($('#items').children().length <= 1) {
                $.ajax({
                    url: '${ACTION}?action=clear',
                    success: function() {
                        $(li).hide(1000, function() {
                            $(li).remove();
                            $('#playlist_container').hide(1000, function() {
                                $('#playlist_container').empty(); 
                            });
                        });
                    },
                    error: function() {
                        $.pnotify( {
                            pnotify_title : '${I18N_ERROR}',
                            pnotify_text : '${I18N_REMOVE_FAILED}: ' + xhr.responseText,
                            pnotify_type : 'error',
                            pnotify_hide: false
                        });
                    }
                });
            } else {
                $.ajax({
                    url: '${ACTION}?action=remove&id=' + $(li).attr('vch:id'),
                    success: function() {
                        $(li).hide(1000, function() {
                            $(li).remove();
                        }); 
                    },
                    error: function(xhr, statusText, error) {
                        $.pnotify( {
                            pnotify_title : '${I18N_ERROR}',
                            pnotify_text : '${I18N_REMOVE_FAILED}: ' + xhr.responseText,
                            pnotify_type : 'error',
                            pnotify_hide: false
                        });
                    }
                });
            }
        }
    </script>
</#if>
</div>
<#include "footer.ftl">