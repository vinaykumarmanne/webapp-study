<content:title>
    <fmt:message key="applications" /> (${fn:length(appGroup.applications)})
</content:title>

<content:section cssId="applicationListPage">
    <div class="section row">
        <p>
            <fmt:message key="to.add.new.content.click.the.button.below" />
        </p>
        
        <table class="bordered highlight">
            <thead>
                <th><fmt:message key="package.name" />/<br /><fmt:message key="label" /></th>
                <th><fmt:message key="version.code" /></th>
                <th><fmt:message key="version.name" /></th>
                <th><fmt:message key="file.size" /></th>
                <th><fmt:message key="status" /></th>
                <th><fmt:message key="contributor" /></th>
            </thead>
            <tbody>
                <c:forEach var="application" items="${appGroup.applications}">
                    <tr class="application">
                        <td>
                            <a href="<spring:url value='/project/${project.id}/app-category/${appCategory.id}/app-group/${appGroup.id}/app/${application.id}/edit' />">
                                <i class="material-icons left">android</i>${application.packageName}
                            </a><br />
                            ${application.latestApplicationVersion.label}
                        </td>
                        <td>
                            ${application.latestApplicationVersion.versionCode}
                        </td>
                        <td>
                            ${application.latestApplicationVersion.versionName}
                        </td>
                        <td>
                            <fmt:formatNumber value="${fn:length(application.latestApplicationVersion.bytes) / 1024 / 1024}" maxFractionDigits="2" />MB
                        </td>
                        <td>
                            ${application.applicationStatus}
                        </td>
                        <td>
                            <div class="chip">
                                <img src="<spring:url value='${application.contributor.imageUrl}' />" alt="${application.contributor.firstName}" /> 
                                <c:out value="${application.contributor.firstName}" />&nbsp;<c:out value="${application.contributor.lastName}" />
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
                
    <div class="fixed-action-btn" style="bottom: 2em; right: 2em;">
        <a href="<spring:url value='/project/${project.id}/app-category/${appCategory.id}/app-group/${appGroup.id}/app/create' />" class="btn-floating btn-large deep-purple lighten-1 tooltipped" data-position="left" data-delay="50" data-tooltip="<fmt:message key="add.application" />"><i class="material-icons">android</i></a>
    </div>
</content:section>
